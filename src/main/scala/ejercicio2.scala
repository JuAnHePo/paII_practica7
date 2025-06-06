package concurrencia

import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable.ListBuffer
import scala.util.Random

class Recursos(rec:Int) {
  private var numRec = rec
  private var esperando = 0
  private var cola: List[Int] = Nil

  // LOCKS Y CONDICIONES

  private val l = ReentrantLock(true)
  private var hayCola = false
  private val chayCola = l.newCondition()
  private val hayRecursos = l.newCondition()

  
  def pidoRecursos(id:Int,num:Int) =  {
    //proceso id solicita num recursos
    l.lock()
    try {
      esperando += 1
      log(s"Proceso $id pide $num recursos. Esperando $esperando")
      if esperando > 0 then
        cola = cola :+ id
        while cola.head != id do chayCola.await()
      hayCola = true
      while numRec < num do hayRecursos.await()
      numRec -= num
      log(s"Proceso $id coge $num recursos. Quedan $numRec")
      hayCola = false
      esperando -= 1
      cola = cola.tail
      chayCola.signal()
    } finally {
      l.unlock()
    }
      
  }

  def libRecursos(id:Int,num:Int) =  {
    //proceso id devuelve num recursos
    l.lock()
    try {
      numRec += num
      hayRecursos.signal()
      log(s"Proceso $id devuelve $num recursos. Quedan $numRec")
    } finally {
      l.unlock()
    }
  }
}
object Ejercicio2 {

  def main(args:Array[String]):Unit = {
    val rec = 5
    val numProc = 10
    val recursos = new Recursos(rec)
    val proceso = new Array[Thread](numProc)
    for (i<-proceso.indices)
      proceso(i) = thread {
        while (true) {
          val r = Random.nextInt(rec)+1
          recursos.pidoRecursos(i,r)
          Thread.sleep(Random.nextInt(100))
          recursos.libRecursos(i,r)
        }
      }
  }
}
