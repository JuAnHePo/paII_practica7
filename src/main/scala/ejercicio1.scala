package concurrencia

import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.util.Random

class Buffer(ncons:Int,tam:Int){
  //ncons-número de consumidores
  //tam-tamaño del buffer
  private val buffer = Array.fill(tam)(0)
  private var pProd = 0 // puntero del productor
  private val pCons = Array.fill(ncons)(0) // punteros de cada consumidor
  private val vecesConsumidas = Array.fill(tam)(ncons)
  private var huecos = tam
  private var datosProducidos = 0
  private var datosConsumidos = Array.fill(ncons)(0)

  // LOCKS Y CONDICIONES

  private val l = ReentrantLock(true) // lock reentrante justo
  private val hayEspacio = l.newCondition() // condición que indica si el buffer está lleno
  private val hayDato = Array.fill(ncons)(l.newCondition()) // array con las condiciones de los consumidores
  
  def nuevoDato(dato:Int) = {
    //el productor pone un nuevo dato
    l.lock()
    try {
      while huecos == 0 do hayEspacio.await() // el productor espera si el buffer está lleno
      buffer(pProd) = dato // añadimos dato al buffer
      datosProducidos += 1
      huecos -= 1
      for (i <- hayDato.indices) hayDato(i).signal()
      pProd = (pProd + 1) % tam // pPuntero avanza y en el caso que se salga de los límites vuelve al comienzo del array
      log(s"Productor almacena $dato: buffer=${buffer.mkString("[", ",", "]")}}")
    } finally {
      l.unlock()
    }
  }

  def extraerDato(id:Int):Int =  {
    l.lock()
    try {
      while datosConsumidos(id) == datosProducidos do hayDato(id).await()
      val pos = pCons(id)
      val valor = buffer(pos)
      vecesConsumidas(pos) -= 1
      datosConsumidos(id) += 1
      pCons(id) = (pos + 1) % tam
      if vecesConsumidas(pos) == 0 then
        buffer(pos) = 0
        huecos += 1
        vecesConsumidas(pos) = ncons
        hayEspacio.signal()
      log(s"Consumidor $id lee $valor: buffer=${buffer.mkString("[", ",", "]")}")
      valor
    } finally {
      l.unlock()
    }
  }
}
object ejercicio1 {

  def main(args:Array[String]):Unit = {
    val ncons = 4
    val tam = 3
    val nIter = 10
    val buffer  = new Buffer(ncons,tam)
    val consumidor = new Array[Thread](ncons)
    for (i<-consumidor.indices)
      consumidor(i) = thread{
        for (j<-0 until nIter)
          val dato = buffer.extraerDato(i)
          Thread.sleep(Random.nextInt(200))
      }
    val productor = thread{
      for (i<-0 until nIter)
        Thread.sleep(Random.nextInt(50))
        buffer.nuevoDato(i+1)
    }
  }

}
