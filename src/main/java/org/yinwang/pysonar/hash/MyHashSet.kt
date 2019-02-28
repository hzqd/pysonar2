package org.yinwang.pysonar.hash

import java.util.AbstractSet


class MyHashSet<E> : AbstractSet<E>, Set<E> {
    @Transient
    private var map: MyHashMap<E, Any>? = null


    constructor(hashFunction: HashFunction, equalFunction: EqualFunction) {
        map = MyHashMap(hashFunction, equalFunction)
    }


    constructor(c: Collection<E>, hashFunction: HashFunction, equalFunction: EqualFunction) {
        map = MyHashMap(Math.max((c.size / .75f).toInt() + 1, 16), hashFunction, equalFunction)
        addAll(c)
    }


    constructor(initialCapacity: Int, loadFactor: Float, hashFunction: HashFunction, equalFunction: EqualFunction) {
        map = MyHashMap(initialCapacity, loadFactor, hashFunction, equalFunction)
    }


    constructor(initialCapacity: Int, hashFunction: HashFunction, equalFunction: EqualFunction) {
        map = MyHashMap(initialCapacity, hashFunction, equalFunction)
    }


    constructor() {
        map = MyHashMap(GenericHashFunction(), GenericEqualFunction())
    }


    override fun iterator(): Iterator<E> {
        return map!!.keys.iterator()
    }


    override fun size(): Int {
        return map!!.size
    }


    override fun isEmpty(): Boolean {
        return map!!.isEmpty()
    }


    override operator fun contains(o: Any?): Boolean {
        return map!!.containsKey(o!!)
    }


    override fun add(e: E?): Boolean {
        return map!!.put(e!!, PRESENT) == null
    }


    override fun remove(o: Any?): Boolean {
        return map!!.remove(o) === PRESENT
    }


    override fun clear() {
        map!!.clear()
    }

    companion object {
        private val PRESENT = Any()
    }
}
