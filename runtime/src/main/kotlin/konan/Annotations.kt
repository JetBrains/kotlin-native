package konan

/**
 * Preserve the function entry point during global optimizations
 */
public annotation class Used


/**
 * Need to be fixed because of reification support.
 */
public annotation class FixmeReified

/**
 * Need to be fixed because of sorting support.
 */
public annotation class FixmeSorting

/**
 * Need to be fixed because of specialization support.
 */
public annotation class FixmeSpecialization

/**
 * Need to be fixed because of sequences support.
 */
public annotation class FixmeSequences

/**
 * Need to be fixed because of variance support.
 */
public annotation class FixmeVariance

/**
 * Need to be fixed because of regular expressions.
 */
public annotation class FixmeRegex

/**
 * Need to be fixed.
 */
public annotation class Fixme
