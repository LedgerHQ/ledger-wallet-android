/**
 *
 * SpvAccountClient
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 25/11/15.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Ledger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package co.ledger.wallet.service.wallet.spv

import java.util
import java.util.concurrent.Executor

import co.ledger.wallet.core.concurrent.AsyncCursor
import co.ledger.wallet.core.utils.logs.{Logger, Loggable}
import co.ledger.wallet.service.wallet.database.model.AccountRow
import co.ledger.wallet.wallet.{DerivationPath, Operation, ExtendedPublicKeyProvider, Account}
import org.bitcoinj.core._
import org.bitcoinj.crypto.{ChildNumber, DeterministicKey}
import co.ledger.wallet.wallet.events.WalletEvents._

import scala.collection.JavaConverters._
import scala.concurrent.Future

class SpvAccountClient(val wallet: SpvWalletClient, data: (AccountRow, Wallet))
  extends Account with Loggable {

  implicit val ec = wallet.ec // Work on the wallet queue

  val row = data._1
  val xpubWatcher = data._2
  val index = row.index

  override def freshPublicAddress(): Future[Address] = Future {
    val reader = wallet.asInstanceOf[SpvWalletClient].databaseReader
    val path = DerivationPath(reader.lastUsedPath(index, 0).getOrElse(s"m/$index'/0/0"))
    val newPath = new DerivationPath(path.parent, path.childNum + 1)
    val childNums = newPath.toBitcoinJList
    childNums.set(0, new ChildNumber(0, true))
    val key = xpubWatcher.getActiveKeychain.getKeyByPath(childNums, true)
    key.toAddress(wallet.networkParameters)
  }

  override def operations(batchSize: Int): Future[AsyncCursor[Operation]] = ???

  override def synchronize(provider: ExtendedPublicKeyProvider): Future[Unit] =
    wallet.synchronize(provider)

  override def xpub(): Future[DeterministicKey] = Future.successful(xpubWatcher.getWatchingKey)

  override def balance(): Future[Coin] = Future.successful(xpubWatcher.getBalance)

  def release(): Unit = {
    xpubWatcher.removeEventListener(_eventListener)
  }

  xpubWatcher.addEventListener(_eventListener, new Executor {
    override def execute(command: Runnable): Unit = ec.execute(command)
  })

  private[this] lazy val _eventListener = new WalletEventListener

  private class WalletEventListener extends AbstractWalletEventListener {

    override def onCoinsReceived(w: Wallet, tx: Transaction, prevBalance: Coin, newBalance: Coin)
    : Unit = {
      super.onCoinsReceived(w, tx, prevBalance, newBalance)
      Logger.d(s"Receive transaction: ${tx.getHashAsString}")
      wallet.notifyAccountTransaction(SpvAccountClient.this, tx)
    }

    override def onCoinsSent(w: Wallet, tx: Transaction, prevBalance: Coin, newBalance:
    Coin): Unit = {
      super.onCoinsSent(w, tx, prevBalance, newBalance)
      Logger.d(s"Send transaction: ${tx.getHashAsString}")
      wallet.notifyAccountTransaction(SpvAccountClient.this, tx)
    }


    override def onTransactionConfidenceChanged(w: Wallet, tx: Transaction): Unit = {
      super.onTransactionConfidenceChanged(w, tx)
      import TransactionConfidence.ConfidenceType._
      tx.getConfidence.getConfidenceType match {
        case DEAD => // TODO: DELETE OPS
          wallet.notifyDeadTransaction(tx)
        case BUILDING =>
        case PENDING =>
        case UNKNOWN =>
      }
    }

    override def onWalletChanged(w: Wallet): Unit = {
      super.onWalletChanged(w)
    }
  }
}