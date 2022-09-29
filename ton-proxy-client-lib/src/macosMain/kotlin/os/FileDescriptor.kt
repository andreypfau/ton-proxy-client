package os

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import unix.setNonBlock

actual class FileDescriptor(
    val fd: Int
) {
    private val _lock = reentrantLock()
    private var _isBlocking by atomic(false)

    val isBlocking get() = _isBlocking

    fun setBlocking() {
        _lock.withLock {
            _isBlocking = true
            setNonBlock(fd, false)
        }
    }

    actual fun close() {

    }
}