/**
 *
 * WalletActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 23/11/15.
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
package co.ledger.wallet.core.base

import android.app.Activity
import android.os.Bundle
import co.ledger.wallet.core.event.MainThreadEventReceiver
import co.ledger.wallet.wallet.Wallet
import co.ledger.wallet.wallet.proxy.WalletProxy
import WalletActivity._

trait WalletActivity extends Activity with MainThreadEventReceiver {

  abstract override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    wallet.asInstanceOf[WalletProxy].bind()
    register(wallet.eventBus)
  }

  abstract override def onDestroy(): Unit = {
    super.onDestroy()
    wallet.asInstanceOf[WalletProxy].unbind()
  }

  lazy val wallet: Wallet = new WalletProxy(this, getIntent.getStringExtra(ExtraWalletName))
}

object WalletActivity {
  val ExtraWalletName = "ExtraWalletName"
}