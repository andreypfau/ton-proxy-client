package unix

import io.ktor.utils.io.errors.*
import platform.posix.errno

private val errorList: List<Triple<Int, String, String>> = listOf(
    Triple(1, "EPERM", "operation not permitted"),
    Triple(2, "ENOENT", "no such file or directory"),
    Triple(3, "ESRCH", "no such process"),
    Triple(4, "EINTR", "interrupted system call"),
    Triple(5, "EIO", "input/output error"),
    Triple(6, "ENXIO", "device not configured"),
    Triple(7, "E2BIG", "argument list too long"),
    Triple(8, "ENOEXEC", "exec format error"),
    Triple(9, "EBADF", "bad file descriptor"),
    Triple(10, "ECHILD", "no child processes"),
    Triple(11, "EDEADLK", "resource deadlock avoided"),
    Triple(12, "ENOMEM", "cannot allocate memory"),
    Triple(13, "EACCES", "permission denied"),
    Triple(14, "EFAULT", "bad address"),
    Triple(15, "ENOTBLK", "block device required"),
    Triple(16, "EBUSY", "resource busy"),
    Triple(17, "EEXIST", "file exists"),
    Triple(18, "EXDEV", "cross-device link"),
    Triple(19, "ENODEV", "operation not supported by device"),
    Triple(20, "ENOTDIR", "not a directory"),
    Triple(21, "EISDIR", "is a directory"),
    Triple(22, "EINVAL", "invalid argument"),
    Triple(23, "ENFILE", "too many open files in system"),
    Triple(24, "EMFILE", "too many open files"),
    Triple(25, "ENOTTY", "inappropriate ioctl for device"),
    Triple(26, "ETXTBSY", "text file busy"),
    Triple(27, "EFBIG", "file too large"),
    Triple(28, "ENOSPC", "no space left on device"),
    Triple(29, "ESPIPE", "illegal seek"),
    Triple(30, "EROFS", "read-only file system"),
    Triple(31, "EMLINK", "too many links"),
    Triple(32, "EPIPE", "broken pipe"),
    Triple(33, "EDOM", "numerical argument out of domain"),
    Triple(34, "ERANGE", "result too large"),
    Triple(35, "EAGAIN", "resource temporarily unavailable"),
    Triple(36, "EINPROGRESS", "operation now in progress"),
    Triple(37, "EALREADY", "operation already in progress"),
    Triple(38, "ENOTSOCK", "socket operation on non-socket"),
    Triple(39, "EDESTADDRREQ", "destination address required"),
    Triple(40, "EMSGSIZE", "message too long"),
    Triple(41, "EPROTOTYPE", "protocol wrong type for socket"),
    Triple(42, "ENOPROTOOPT", "protocol not available"),
    Triple(43, "EPROTONOSUPPORT", "protocol not supported"),
    Triple(44, "ESOCKTNOSUPPORT", "socket type not supported"),
    Triple(45, "ENOTSUP", "operation not supported"),
    Triple(46, "EPFNOSUPPORT", "protocol family not supported"),
    Triple(47, "EAFNOSUPPORT", "address family not supported by protocol family"),
    Triple(48, "EADDRINUSE", "address already in use"),
    Triple(49, "EADDRNOTAVAIL", "can't assign requested address"),
    Triple(50, "ENETDOWN", "network is down"),
    Triple(51, "ENETUNREACH", "network is unreachable"),
    Triple(52, "ENETRESET", "network dropped connection on reset"),
    Triple(53, "ECONNABORTED", "software caused connection abort"),
    Triple(54, "ECONNRESET", "connection reset by peer"),
    Triple(55, "ENOBUFS", "no buffer space available"),
    Triple(56, "EISCONN", "socket is already connected"),
    Triple(57, "ENOTCONN", "socket is not connected"),
    Triple(58, "ESHUTDOWN", "can't send after socket shutdown"),
    Triple(59, "ETOOMANYREFS", "too many references: can't splice"),
    Triple(60, "ETIMEDOUT", "operation timed out"),
    Triple(61, "ECONNREFUSED", "connection refused"),
    Triple(62, "ELOOP", "too many levels of symbolic links"),
    Triple(63, "ENAMETOOLONG", "file name too long"),
    Triple(64, "EHOSTDOWN", "host is down"),
    Triple(65, "EHOSTUNREACH", "no route to host"),
    Triple(66, "ENOTEMPTY", "directory not empty"),
    Triple(67, "EPROCLIM", "too many processes"),
    Triple(68, "EUSERS", "too many users"),
    Triple(69, "EDQUOT", "disc quota exceeded"),
    Triple(70, "ESTALE", "stale NFS file handle"),
    Triple(71, "EREMOTE", "too many levels of remote in path"),
    Triple(72, "EBADRPC", "RPC struct is bad"),
    Triple(73, "ERPCMISMATCH", "RPC version wrong"),
    Triple(74, "EPROGUNAVAIL", "RPC prog. not avail"),
    Triple(75, "EPROGMISMATCH", "program version wrong"),
    Triple(76, "EPROCUNAVAIL", "bad procedure for program"),
    Triple(77, "ENOLCK", "no locks available"),
    Triple(78, "ENOSYS", "function not implemented"),
    Triple(79, "EFTYPE", "inappropriate file type or format"),
    Triple(80, "EAUTH", "authentication error"),
    Triple(81, "ENEEDAUTH", "need authenticator"),
    Triple(82, "EPWROFF", "device power is off"),
    Triple(83, "EDEVERR", "device error"),
    Triple(84, "EOVERFLOW", "value too large to be stored in data type"),
    Triple(85, "EBADEXEC", "bad executable (or shared library)"),
    Triple(86, "EBADARCH", "bad CPU type in executable"),
    Triple(87, "ESHLIBVERS", "shared library version mismatch"),
    Triple(88, "EBADMACHO", "malformed Mach-o file"),
    Triple(89, "ECANCELED", "operation canceled"),
    Triple(90, "EIDRM", "identifier removed"),
    Triple(91, "ENOMSG", "no message of desired type"),
    Triple(92, "EILSEQ", "illegal byte sequence"),
    Triple(93, "ENOATTR", "attribute not found"),
    Triple(94, "EBADMSG", "bad message"),
    Triple(95, "EMULTIHOP", "EMULTIHOP (Reserved)"),
    Triple(96, "ENODATA", "no message available on STREAM"),
    Triple(97, "ENOLINK", "ENOLINK (Reserved)"),
    Triple(98, "ENOSR", "no STREAM resources"),
    Triple(99, "ENOSTR", "not a STREAM"),
    Triple(100, "EPROTO", "protocol error"),
    Triple(101, "ETIME", "STREAM ioctl timeout"),
    Triple(102, "EOPNOTSUPP", "operation not supported on socket"),
    Triple(103, "ENOPOLICY", "policy not found"),
    Triple(104, "ENOTRECOVERABLE", "state not recoverable"),
    Triple(105, "EOWNERDEAD", "previous owner died"),
    Triple(106, "EQFULL", "interface output queue is full"),
)

fun errnoString(e: Int = errno): String? {
    val i = errorList.binarySearch { it.first.compareTo(e) }
    if (i in errorList.indices && errorList[i].first == e) {
        return "${errorList[i].second} ($e): ${errorList[i].third}"
    }
    return null
}

internal inline fun Int.check(
    block: (Int) -> Boolean = { it >= 0 }
): Int {
    if (!block(this)) {
        throw PosixException()
    }
    return this
}

fun PosixException(): PosixException {
    val error = PosixException.forErrno()
    if (error is PosixException.PosixErrnoException) {
        return PosixException.PosixErrnoException(errno, errnoString() ?: "POSIX error $errno")
    }
    return error
}