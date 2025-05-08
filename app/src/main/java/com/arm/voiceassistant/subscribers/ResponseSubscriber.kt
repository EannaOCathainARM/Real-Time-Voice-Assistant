/*
 * SPDX-FileCopyrightText: Copyright 2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arm.voiceassistant.subscribers

import android.util.Log
import com.arm.voiceassistant.utils.Constants.VOICE_ASSISTANT_TAG
import com.arm.voiceassistant.utils.Utils.responseComplete
import com.arm.voiceassistant.viewmodels.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.Flow
import java.util.concurrent.Flow.Subscriber

class ResponseSubscriber(mainViewModel: MainViewModel) : Subscriber<String> {
    private var subscription: Flow.Subscription? = null
    private var subscribedViewModel = mainViewModel
    private var coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onSubscribe(subscription: Flow.Subscription?) {
        this.subscription = subscription
        Log.d(VOICE_ASSISTANT_TAG, "Subscribed")
        subscription!!.request(Long.MAX_VALUE)
    }

    override fun onError(throwable: Throwable?) {
        Log.d(VOICE_ASSISTANT_TAG, "Error")
    }

    override fun onComplete() {
        Log.d(VOICE_ASSISTANT_TAG, "Response Complete!")
        subscribedViewModel.updateToIdleState()

    }

    override fun onNext(item: String?) {
        if (item!=null) {
            coroutineScope.launch {
                if (!responseComplete(item)) {
                    subscribedViewModel.updateResponseFieldCallback(item)
                }
                subscribedViewModel.generatedResponseCallback(item)
            }
        }

        Log.d(VOICE_ASSISTANT_TAG, "Next item - $item")
    }

    fun cancel() {
        subscription?.cancel()
        coroutineScope.cancel()
    }
}