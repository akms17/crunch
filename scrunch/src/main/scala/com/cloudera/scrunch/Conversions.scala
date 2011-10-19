/**
 * Copyright (c) 2011, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.scrunch

import com.cloudera.crunch.{PCollection => JCollection, PGroupedTable => JGroupedTable, PTable => JTable}
import com.cloudera.crunch.{Pair => CPair, Tuple3 => CTuple3, Tuple4 => CTuple4, Tuple => CTuple, TupleN => CTupleN}
import com.cloudera.crunch.`type`.{PType, PTypeFamily};
import java.lang.{Boolean => JBoolean, Double => JDouble, Float => JFloat, Integer => JInteger, Long => JLong}
import java.nio.ByteBuffer

object Conversions {

  implicit def jtable2ptable[K, V](jtable: JTable[K, V]) = jtable match {
    case x: PTable[K, V] => x
    case _ => new PTable[K, V](jtable)
  }
  
  implicit def jcollect2pcollect[S](jcollect: JCollection[S]) = jcollect match {
    case x: PCollection[S] => x
    case _ => new PCollection[S](jcollect)
  }
  
  implicit def jgrouped2pgrouped[K, V](jgrouped: JGroupedTable[K, V]) = jgrouped match {
    case x: PGroupedTable[K, V] => x
    case _ => new PGroupedTable[K, V](jgrouped)
  }
  
  def manifest2PType[S](m: ClassManifest[S], ptf: PTypeFamily): PType[_] = {
    def conv(x: OptManifest[_]): PType[_] = manifest2PType(x.asInstanceOf[ClassManifest[_]], ptf)
    val clazz = m.erasure
    if (classOf[java.lang.String].equals(clazz)) {
      ptf.strings()
    } else if (classOf[Double].equals(clazz) || classOf[JDouble].equals(clazz)) {
      ptf.doubles()
    } else if (classOf[Boolean].equals(clazz) || classOf[JBoolean].equals(clazz)) {
      ptf.booleans()
    } else if (classOf[Float].equals(clazz) || classOf[JFloat].equals(clazz)) {
      ptf.floats()
    } else if (classOf[Int].equals(clazz) || classOf[JInteger].equals(clazz)) {
      ptf.ints()
    } else if (classOf[Long].equals(clazz) || classOf[JLong].equals(clazz)) {
      ptf.longs()
    } else if (classOf[ByteBuffer].equals(clazz)) {
      ptf.bytes()
    } else if (classOf[Iterable[_]].isAssignableFrom(clazz)) {
      ptf.collections(conv(m.typeArguments(0)))
    } else if (classOf[Product].isAssignableFrom(clazz)) {
      val pt = m.typeArguments.map(conv)
      pt.size match {
        case 1 => pt(0)
        case 2 => ptf.pairs(pt(0), pt(1))
        case 3 => ptf.triples(pt(0), pt(1), pt(2))
        case 4 => ptf.quads(pt(0), pt(1), pt(2), pt(3))
        case _ => ptf.tuples(pt : _*)
      }
    } else {
      println("Could not match manifest: " + m + " with class: " + clazz)
      ptf.records(clazz)
    }
  }

  def s2c(obj: Any): Any = obj match {
    case x: Tuple2[_, _] =>  CPair.of(s2c(x._1), s2c(x._2))
    case x: Tuple3[_, _, _] => new CTuple3(s2c(x._1), s2c(x._2), s2c(x._3))
    case x: Tuple4[_, _, _, _] => new CTuple4(s2c(x._1), s2c(x._2), s2c(x._3), s2c(x._4))
    case x: Product => new CTupleN(x.productIterator.map(s2c).toList.toArray)
    case _ => obj
  }

  def c2s(obj: Any): Any = obj match {
    case x: CTuple => {
      val v = (0 until x.size).map((i: Int) => c2s(x.get(i))).toArray
      v.length match {
       case 2 => Tuple2(v(0), v(1))
       case 3 => Tuple3(v(0), v(1), v(2))
       case 4 => Tuple4(v(0), v(1), v(2), v(3))
       case 5 => Tuple5(v(0), v(1), v(2), v(3), v(4))
       case 6 => Tuple6(v(0), v(1), v(2), v(3), v(4), v(5))
       case 7 => Tuple7(v(0), v(1), v(2), v(3), v(4), v(5), v(6))
       case 8 => Tuple8(v(0), v(1), v(2), v(3), v(4), v(5), v(6), v(7))
       case 9 => Tuple9(v(0), v(1), v(2), v(3), v(4), v(5), v(6), v(7), v(8))
       case 10 => Tuple10(v(0), v(1), v(2), v(3), v(4), v(5), v(6), v(7), v(8), v(9))
       case _ => { println("Seriously? A " + v.length + " tuple?"); obj }
     }
    }
    case _ => obj
  }
}
