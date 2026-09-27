package com.miaouss90.tellocontroler.controller
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicReference

class RcSafetyLoop(private val send:(RcInput)->Unit) {
 companion object { const val PERIOD_MS=50L; const val STALE_MS=250L }
 private data class Timed(val input:RcInput,val at:Long)
 private val latest=AtomicReference(Timed(RcInput(),0)); private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
 fun start(){ scope.launch{ while(isActive){ val n=System.currentTimeMillis(); val v=latest.get(); send(if(n-v.at<=STALE_MS)v.input else RcInput()); delay(PERIOD_MS) }}}
 fun update(i:RcInput){latest.set(Timed(i,System.currentTimeMillis()))}
 fun neutral(){latest.set(Timed(RcInput(),System.currentTimeMillis()))}
 fun stop(){neutral();send(RcInput());scope.cancel()}
}