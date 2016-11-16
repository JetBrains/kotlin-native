package kotlin

public class Error : Throwable {

    /**
     * Constructs a new error with `null` as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to [.initCause].
     */
    constructor() : super() {
    }

    /**
     * Constructs a new error with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to [.initCause].
     */
    constructor(message: String) : super(message) {
    }

    /**
     * Constructs a new error with the specified detail message and
     * cause.
     *
     *Note that the detail message associated with
     * `cause` is *not* automatically incorporated in
     * this error's detail message.

     * @param  message the detail message (which is saved for later retrieval
     * *         by the [.getMessage] method).
     * *
     * @param  cause the cause (which is saved for later retrieval by the
     * *         [.getCause] method).  (A `null` value is
     * *         permitted, and indicates that the cause is nonexistent or
     * *         unknown.)
     * *
     */
    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    /**
     * Constructs a new error with the specified cause and a detail
     * message of `(cause==null ? null : cause.toString())` (which
     * typically contains the class and detail message of `cause`).
     * This constructor is useful for errors that are little more than
     * wrappers for other throwables.

     * @param  cause the cause (which is saved for later retrieval by the
     * *         [.getCause] method).  (A `null` value is
     * *         permitted, and indicates that the cause is nonexistent or
     * *         unknown.)
     */
    constructor(cause: Throwable) : super(cause) {
    }
}

public open class Exception : Throwable {

    /**
     * Constructs a new exception with `null` as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to [.initCause].
     */
    constructor() : super() {
    }

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to [.initCause].

     * @param   message   the detail message. The detail message is saved for
     * *          later retrieval by the [.getMessage] method.
     */
    constructor(message: String) : super(message) {
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     *
     *Note that the detail message associated with
     * `cause` is *not* automatically incorporated in
     * this exception's detail message.

     * @param  message the detail message (which is saved for later retrieval
     * *         by the [.getMessage] method).
     * *
     * @param  cause the cause (which is saved for later retrieval by the
     * *         [.getCause] method).  (A <tt>null</tt> value is
     * *         permitted, and indicates that the cause is nonexistent or
     * *         unknown.)
     * *
     */
    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    /**
     * Constructs a new exception with the specified cause and a detail
     * message of <tt>(cause==null ? null : cause.toString())</tt> (which
     * typically contains the class and detail message of <tt>cause</tt>).
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example, [ ]).

     * @param  cause the cause (which is saved for later retrieval by the
     * *         [.getCause] method).  (A <tt>null</tt> value is
     * *         permitted, and indicates that the cause is nonexistent or
     * *         unknown.)
     * *
     */
    constructor(cause: Throwable) : super(cause) {
    }
}

public class RuntimeException : Exception {

    /** Constructs a new runtime exception with `null` as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to [.initCause].
     */
    constructor() : super() {
    }

    /** Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to [.initCause].

     * @param   message   the detail message. The detail message is saved for
     * *          later retrieval by the [.getMessage] method.
     */
    constructor(message: String) : super(message) {
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.
     *
     *Note that the detail message associated with
     * `cause` is *not* automatically incorporated in
     * this runtime exception's detail message.

     * @param  message the detail message (which is saved for later retrieval
     * *         by the [.getMessage] method).
     * *
     * @param  cause the cause (which is saved for later retrieval by the
     * *         [.getCause] method).  (A <tt>null</tt> value is
     * *         permitted, and indicates that the cause is nonexistent or
     * *         unknown.)
     */
    constructor(message: String, cause: Throwable) : super(message, cause) {
    }

    /** Constructs a new runtime exception with the specified cause and a
     * detail message of <tt>(cause==null ? null : cause.toString())</tt>
     * (which typically contains the class and detail message of
     * <tt>cause</tt>).  This constructor is useful for runtime exceptions
     * that are little more than wrappers for other throwables.

     * @param  cause the cause (which is saved for later retrieval by the
     * *         [.getCause] method).  (A <tt>null</tt> value is
     * *         permitted, and indicates that the cause is nonexistent or
     * *         unknown.)
     */
    constructor(cause: Throwable) : super(cause) {
    }
}

class NullPointerException : RuntimeException {

    /**
     * Constructs a `NullPointerException` with no detail message.
     */
    constructor() : super() {
    }

    /**
     * Constructs a `NullPointerException` with the specified
     * detail message.

     * @param   s   the detail message.
     */
    constructor(s: String) : super(s) {
    }
}
