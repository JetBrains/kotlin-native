/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.gitchurn


import kotlinx.cinterop.*
import libgit2.*

/**
 * This class provides revwalk functionality by wrapping imported C functions to be the one entity
 */
class GitRevwalk (private val repository_handle: CPointer<git_repository>, val sort: git_sort_t) {
    private val arena = Arena()
    private val handle = memScoped {
        val loc = allocPointerTo<git_revwalk>()
        git_revwalk_new(loc.ptr, repository_handle).errorCheck()
        loc.value!!
    }

    fun close() {
        git_revwalk_free(handle)
        arena.clear()
    }

    fun repush(oidToStartFrom: String = "") { // oidToStartFrom determines, whether revwalk will start from the HEAD reference, or some exact commit.
        git_revwalk_reset(handle)
        git_revwalk_sorting(walk = handle, sort_mode = sort)
        when(oidToStartFrom){
            "" -> git_revwalk_push_head(handle)
            else -> memScoped {
                val oid = alloc<git_oid>()
                git_oid_fromstr(oid.ptr, oidToStartFrom)
                git_revwalk_push(handle, oid.ptr)
            }
        }
    }

    fun sortMode(i:UInt){
        git_revwalk_sorting(walk = handle, sort_mode = i)
    }

    fun nextCommit() : GitCommit? {
        val commitPtr = arena.allocPointerTo<git_commit>()// here i can take iterated oids pointing to different commits, sorted by ^
        val o = arena.alloc<git_oid>()
        return when(val n = git_revwalk_next(o.ptr, handle)){
            0 -> {
                git_commit_lookup(commitPtr.ptr, repository_handle, o.ptr).errorCheck()
                val commit = commitPtr.value!!
                GitCommit(repository_handle, commit)
            }
            GIT_ITEROVER -> null
            else -> throw Exception("Unexpected result code $n")
        }
    }
}