package org.yinwang.pysonar.hash

import java.util.*

class MyHashMap<K, V> @JvmOverloads constructor(initialCapacity: Int = DEFAULT_INITIAL_CAPACITY, internal val loadFactor: Float = DEFAULT_LOAD_FACTOR, internal var hashFunction: HashFunction = GenericHashFunction(), internal var equalFunction: EqualFunction = GenericEqualFunction()) : AbstractMap<K, V>(), Map<K, V> {
    internal var table: Array<Entry<K, V>>
    internal var size: Int = 0
    internal var threshold: Int = 0
    internal var modCount: Int = 0

    private var entrySet: Set<Entry<K, V>>? = null
    @Volatile
    internal var keySet: Set<K>? = null
    @Volatile
    internal var values: Collection<V>? = null


    init {
        var initialCapacity = initialCapacity
        kotlin.require(initialCapacity >= 0) { "Illegal initial capacity: $initialCapacity" }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY
        }
        kotlin.require(!(loadFactor <= 0 || java.lang.Float.isNaN(loadFactor))) { "Illegal load factor: $loadFactor" }

        this.table = arrayOfNulls<Entry<*, *>>(0)
        threshold = initialCapacity
    }


    constructor(initialCapacity: Int, hashFunction: HashFunction, equalFunction: EqualFunction) : this(initialCapacity, DEFAULT_LOAD_FACTOR, hashFunction, equalFunction) {}


    constructor(hashFunction: HashFunction, equalFunction: EqualFunction) : this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, hashFunction, equalFunction) {}


    constructor(m: Map<out K, V>, hashFunction: HashFunction, equalFunction: EqualFunction) : this(Math.max((m.size / DEFAULT_LOAD_FACTOR).toInt() + 1, DEFAULT_INITIAL_CAPACITY),
            DEFAULT_LOAD_FACTOR, hashFunction, equalFunction) {
        putAll(m)
    }


    private fun initTable(size: Int) {
        var size = size
        size = roundup(size)
        threshold = Math.min(size * loadFactor, (MAXIMUM_CAPACITY + 1).toFloat()).toInt()
        table = arrayOfNulls<Entry<*, *>>(size)
    }


    internal fun hash(k: Any): Int {
        var h = hashFunction.hash(k)
        h = h xor (h.ushr(20) xor h.ushr(12))
        return h xor h.ushr(7) xor h.ushr(4)
    }


    override fun size(): Int {
        return size
    }


    override fun isEmpty(): Boolean {
        return size == 0
    }


    override operator fun get(key: Any): V? {
        val entry = getEntry(key)
        return entry?.value
    }


    override fun containsKey(key: Any): Boolean {
        return getEntry(key) != null
    }


    internal fun getEntry(key: Any): Entry<K, V>? {
        if (isEmpty()) {
            return null
        }

        val h = hash(key)
        var e: Entry<K, V>? = table[slot(h, table.size)]
        while (e != null) {
            if (equalFunction.equals(e.key, key)) {
                return e
            }
            e = e.next
        }
        return null
    }


    override fun put(key: K, value: V?): V? {
        if (isEmpty()) {
            initTable(threshold)
        }
        val h = hash(key)
        val i = slot(h, table.size)
        var e: Entry<K, V>? = table[i]
        while (e != null) {
            if (equalFunction.equals(e.key, key)) {
                val oldValue = e.value
                e.value = value
                return oldValue
            }
            e = e.next
        }

        modCount++
        addEntry(h, key, value, i)
        return null
    }


    internal fun resize(size: Int) {
        if (size > MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE
        } else {
            val table2 = arrayOfNulls<Entry<*, *>>(size)
            for (e in table) {
                while (e != null) {
                    val next = e.next
                    val i = slot(e.hash, size)
                    e.next = table2[i]
                    table2[i] = e
                    e = next
                }
            }
            table = table2
            threshold = Math.min(size * loadFactor, (MAXIMUM_CAPACITY + 1).toFloat()).toInt()
        }
    }


    override fun putAll(m: Map<out K, V>) {
        for ((key, value) in m) {
            put(key, value)
        }
    }


    override fun remove(key: Any?): V? {
        val e = removeEntry(key)
        return e?.value
    }


    internal fun removeEntry(key: Any?): Entry<K, V>? {
        if (isEmpty()) {
            return null
        }
        val h = if (key == null) 0 else hash(key)
        val i = slot(h, table.size)
        var prev = table[i]
        var e: Entry<K, V>? = prev

        while (e != null) {
            val next = e.next
            if (equalFunction.equals(e.key, key)) {
                modCount++
                size--
                if (prev === e) {
                    table[i] = next
                } else {
                    prev.next = next
                }
                return e
            }
            prev = e
            e = next
        }

        return e
    }


    internal fun removeMapping(entry: Entry<*, *>): Entry<K, V>? {
        val key = entry.key
        val hash = if (key == null) 0 else hash(key)
        val i = slot(hash, table.size)
        var prev = table[i]
        var e: Entry<K, V>? = prev

        while (e != null) {
            val next = e.next
            if (e.hash == hash && e == entry) {
                modCount++
                size--
                if (prev === e) {
                    table[i] = next
                } else {
                    prev.next = next
                }
                return e
            }
            prev = e
            e = next
        }

        return e
    }


    override fun clear() {
        modCount++
        Arrays.fill(table, null)
        size = 0
    }


    override fun containsValue(value: Any?): Boolean {
        if (value == null) {
            return containsNullValue()
        }

        val tab = table
        for (i in tab.indices) {
            var e: Entry<*, *>? = tab[i]
            while (e != null) {
                if (equalFunction.equals(value, e.value)) {
                    return true
                }
                e = e.next
            }
        }
        return false
    }


    private fun containsNullValue(): Boolean {
        val tab = table
        for (i in tab.indices) {
            var e: Entry<*, *>? = tab[i]
            while (e != null) {
                if (e.value == null) {
                    return true
                }
                e = e.next
            }
        }
        return false
    }


    internal class Entry<K, V>(var hash: Int, val key: K, var value: V?, var next: Entry<K, V>) : Entry<K, V> {


        override fun getKey(): K {
            return key
        }


        override fun getValue(): V? {
            return value
        }


        override fun setValue(newValue: V): V {
            val oldValue = value
            value = newValue
            return oldValue
        }


        override fun equals(o: Any?): Boolean {
            if (o !is Entry<*, *>) {
                return false
            }
            val e = o as Entry<*, *>?
            val k1 = key
            val k2 = e!!.key
            if (k1 === k2 || k1 == k2) {
                val v1 = value
                val v2 = e.value
                if (v1 === v2 || v1 != null && v1 == v2) {
                    return true
                }
            }
            return false
        }


        override fun hashCode(): Int {
            return Objects.hashCode(key) xor Objects.hashCode(value)
        }


        override fun toString(): String {
            return key.toString() + "=" + value
        }
    }


    internal fun addEntry(h: Int, key: K, value: V?, bucketIndex: Int) {
        var h = h
        var bucketIndex = bucketIndex
        if (size >= threshold && table[bucketIndex] != null) {
            resize(2 * table.size)
            h = hash(key)
            bucketIndex = slot(h, table.size)
        }

        createEntry(h, key, value, bucketIndex)
    }


    internal fun createEntry(hash: Int, key: K, value: V?, bucketIndex: Int) {
        val e = table[bucketIndex]
        table[bucketIndex] = Entry(hash, key, value, e)
        size++
    }


    private abstract inner class HashIterator<E> internal constructor() : Iterator<E> {
        internal var next: Entry<K, V>? = null
        internal var expectedModCount: Int = 0
        internal var index: Int = 0
        internal var current: Entry<K, V>? = null


        init {
            expectedModCount = modCount
            if (size > 0) {
                val t = table
                for (i in t.indices) {
                    if (t[i] != null) {
                        next = t[i]
                        index = i
                        break
                    }
                }
            }
        }


        override fun hasNext(): Boolean {
            return next != null
        }


        internal fun nextEntry(): Entry<K, V> {
            if (modCount != expectedModCount) {
                throw ConcurrentModificationException()
            }
            if (next == null) {
                throw NoSuchElementException()
            }

            val ret = next
            next = ret!!.next
            if (next == null) {
                val t = table
                for (i in index + 1 until t.size) {
                    if (t[i] != null) {
                        index = i
                        next = t[i]
                        break
                    }
                }
            }
            current = ret
            return ret
        }


        override fun remove() {
            kotlin.checkNotNull(current)
            if (modCount != expectedModCount) {
                throw ConcurrentModificationException()
            }
            val k = current!!.key
            current = null
            this@MyHashMap.removeEntry(k)
            expectedModCount = modCount
        }
    }

    private inner class ValueIterator : HashIterator<V>() {
        override fun next(): V? {
            return nextEntry().value
        }
    }

    private inner class KeyIterator : HashIterator<K>() {
        override fun next(): K {
            return nextEntry().key
        }
    }

    private inner class EntryIterator : HashIterator<Entry<K, V>>() {
        override fun next(): Entry<K, V> {
            return nextEntry()
        }
    }


    override fun keySet(): Set<K> {
        if (keySet == null) {
            keySet = KeySet()
        }
        return keySet
    }


    private inner class KeySet : AbstractSet<K>() {
        override fun iterator(): Iterator<K> {
            return KeyIterator()
        }


        override fun size(): Int {
            return size
        }


        override operator fun contains(o: Any?): Boolean {
            return containsKey(o!!)
        }


        override fun remove(o: Any?): Boolean {
            return this@MyHashMap.removeEntry(o) != null
        }


        override fun clear() {
            this@MyHashMap.clear()
        }
    }


    override fun values(): Collection<V> {
        if (values == null) {
            values = Values()
        }
        return values
    }


    private inner class Values : AbstractCollection<V>() {
        override fun iterator(): Iterator<V> {
            return ValueIterator()
        }


        override fun size(): Int {
            return size
        }


        override operator fun contains(o: Any?): Boolean {
            return containsValue(o)
        }


        override fun clear() {
            this@MyHashMap.clear()
        }
    }


    override fun entrySet(): Set<Entry<K, V>> {
        if (entrySet == null) {
            entrySet = EntrySet()
        }
        return entrySet
    }


    private inner class EntrySet : AbstractSet<Entry<K, V>>() {
        override fun iterator(): Iterator<Entry<K, V>> {
            return EntryIterator()
        }


        override operator fun contains(o: Any?): Boolean {
            if (o !is Entry<*, *>) {
                return false
            }
            val e = o as Entry<K, V>?
            val candidate = getEntry(e!!.key)
            return candidate != null && candidate == e
        }


        override fun remove(o: Any?): Boolean {
            return if (isEmpty() || o !is Entry<*, *>) {
                false
            } else removeMapping((o as Entry<*, *>?)!!) != null
        }


        override fun size(): Int {
            return size
        }


        override fun clear() {
            this@MyHashMap.clear()
        }
    }

    companion object {
        internal val DEFAULT_INITIAL_CAPACITY = 1
        internal val MAXIMUM_CAPACITY = 1 shl 30
        internal val DEFAULT_LOAD_FACTOR = 0.75f


        private fun roundup(number: Int): Int {
            if (number >= MAXIMUM_CAPACITY) {
                return MAXIMUM_CAPACITY
            }
            var n = 1
            while (n < number) {
                n = n shl 1
            }
            return n
        }


        internal fun slot(h: Int, length: Int): Int {
            return h and length - 1
        }
    }
}
