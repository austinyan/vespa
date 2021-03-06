// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <memory>

namespace document {
class Bucket;
class Document;
class DocumentUpdate;
class DocumentId;
}

namespace feedbm {

class PendingTracker;

/*
 * Interface class for benchmark feed handler.
 */
class IBmFeedHandler
{
public:
    virtual ~IBmFeedHandler() = default;
    virtual void put(const document::Bucket& bucket, std::unique_ptr<document::Document> document, uint64_t timestamp, PendingTracker& tracker) = 0;
    virtual void update(const document::Bucket& bucket, std::unique_ptr<document::DocumentUpdate> document_update, uint64_t timestamp, PendingTracker& tracker) = 0;
    virtual void remove(const document::Bucket& bucket, const document::DocumentId& document_id,  uint64_t timestamp, PendingTracker& tracker) = 0;
};

}
