// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

#include <vespamalloc/malloc/common.h>
#include <vespamalloc/malloc/allocchunk.h>
#include <vespamalloc/malloc/datasegment.h>
#include <algorithm>

#define USE_STAT2(a) a

namespace vespamalloc {

template <typename MemBlockPtrT>
class AllocPoolT
{
public:
    typedef AFList<MemBlockPtrT> ChunkSList;
    AllocPoolT(DataSegment<MemBlockPtrT> & ds);
    ~AllocPoolT();

    ChunkSList *getFree(SizeClassT sc, size_t minBlocks);
    ChunkSList *exchangeFree(SizeClassT sc, ChunkSList * csl);
    ChunkSList *exchangeAlloc(SizeClassT sc, ChunkSList * csl);
    ChunkSList *exactAlloc(size_t exactSize, SizeClassT sc, ChunkSList * csl) __attribute__((noinline));
    ChunkSList *returnMemory(SizeClassT sc, ChunkSList * csl) __attribute__((noinline));

    DataSegment<MemBlockPtrT> & dataSegment()      { return _dataSegment; }
    void enableThreadSupport() __attribute__((noinline));

    static void setParams(size_t alwaysReuseLimit, size_t threadCacheLimit) {
        _alwaysReuseLimit = alwaysReuseLimit;
        _threadCacheLimit = threadCacheLimit;
    }

    void info(FILE * os, size_t level=0) __attribute__((noinline));
private:
    ChunkSList * getFree(SizeClassT sc) __attribute__((noinline));
    ChunkSList * getAlloc(SizeClassT sc) __attribute__((noinline));
    ChunkSList * malloc(const Guard & guard, SizeClassT sc) __attribute__((noinline));
    ChunkSList * getChunks(const Guard & guard, size_t numChunks) __attribute__((noinline));
    ChunkSList * allocChunkList(const Guard & guard) __attribute__((noinline));
    AllocPoolT(const AllocPoolT & ap);
    AllocPoolT & operator = (const AllocPoolT & ap);

    class AllocFree
    {
    public:
        AllocFree() : _full(), _empty() { }
        typename ChunkSList::HeadPtr _full;
        typename ChunkSList::HeadPtr _empty;
    };
    class Stat
    {
    public:
        Stat() : _getAlloc(0),
                 _getFree(0),
                 _exchangeAlloc(0),
                 _exchangeFree(0),
                 _exactAlloc(0),
                 _return(0),_malloc(0) { }
        size_t _getAlloc;
        size_t _getFree;
        size_t _exchangeAlloc;
        size_t _exchangeFree;
        size_t _exactAlloc;
        size_t _return;
        size_t _malloc;
        bool isUsed()       const {
            // Do not count _getFree.
            return (_getAlloc || _exchangeAlloc || _exchangeFree || _exactAlloc || _return || _malloc);
        }
    };

    Mutex                       _mutex;
    ChunkSList                * _chunkPool;
    AllocFree                   _scList[NUM_SIZE_CLASSES] VESPALIB_ATOMIC_TAGGEDPTR_ALIGNMENT;
    DataSegment<MemBlockPtrT> & _dataSegment;
    size_t                      _getChunks;
    size_t                      _getChunksSum;
    size_t                      _allocChunkList;
    Stat                        _stat[NUM_SIZE_CLASSES];
    static size_t               _threadCacheLimit __attribute__((visibility("hidden")));
    static size_t               _alwaysReuseLimit __attribute__((visibility("hidden")));
};

}
