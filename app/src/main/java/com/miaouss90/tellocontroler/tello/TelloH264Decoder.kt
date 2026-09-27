package com.miaouss90.tellocontroler.tello

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Low-latency AVC decoder for the Tello H.264 elementary stream.
 * UDP chunks are accumulated and split on Annex-B start codes.
 */
class TelloH264Decoder(private val surface:Surface) {
 private var codec:MediaCodec?=null
 private val running=AtomicBoolean(false)
 private val buffer=ByteArrayOutputStream()
 private var pts=0L

 fun start(){
  if(running.getAndSet(true)) return
  codec=MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply{
   val format=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,960,720)
   format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,1024*1024)
   configure(format,surface,null,0); start()
  }
 }
 @Synchronized fun offer(chunk:ByteArray){
  if(!running.get())return
  buffer.write(chunk)
  val data=buffer.toByteArray()
  val starts=findStarts(data)
  if(starts.size<2)return
  for(i in 0 until starts.size-1) queue(data.copyOfRange(starts[i],starts[i+1]))
  val tail=data.copyOfRange(starts.last(),data.size)
  buffer.reset(); buffer.write(tail)
  drain()
 }
 private fun findStarts(d:ByteArray):List<Int>{
  val r=mutableListOf<Int>(); var i=0
  while(i<d.size-3){
   if(d[i].toInt()==0 && d[i+1].toInt()==0 && ((d[i+2].toInt()==1) || (d[i+2].toInt()==0 && d[i+3].toInt()==1))){r+=i;i+=3}else i++
  }; return r
 }
 private fun queue(nal:ByteArray){
  val c=codec?:return
  val idx=c.dequeueInputBuffer(0)
  if(idx>=0){ c.getInputBuffer(idx)?.apply{clear();put(nal)}; c.queueInputBuffer(idx,0,nal.size,pts,0); pts+=33_333 }
 }
 private fun drain(){
  val c=codec?:return; val info=MediaCodec.BufferInfo()
  while(true){ val out=c.dequeueOutputBuffer(info,0); if(out<0)break; c.releaseOutputBuffer(out,true) }
 }
 fun stop(){running.set(false);runCatching{codec?.stop()};runCatching{codec?.release()};codec=null;buffer.reset()}
}
