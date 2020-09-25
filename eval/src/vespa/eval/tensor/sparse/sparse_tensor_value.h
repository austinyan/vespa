// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include "sparse_tensor_address_ref.h"
#include <vespa/eval/eval/value.h>
#include <vespa/eval/tensor/types.h>
#include <vespa/vespalib/stllike/hash_map.h>
#include <vespa/vespalib/stllike/string.h>
#include <vespa/vespalib/util/stash.h>

namespace vespalib::tensor {

struct SparseTensorValueIndex : public vespalib::eval::Value::Index
{
    using View = vespalib::eval::Value::Index::View;
    using SubspaceMap = hash_map<SparseTensorAddressRef, uint32_t, hash<SparseTensorAddressRef>,
                                 std::equal_to<>, hashtable_base::and_modulator>;
    SubspaceMap map;
    size_t num_mapped_dims;
    explicit SparseTensorValueIndex(size_t num_mapped_dims_in);
    ~SparseTensorValueIndex();
    size_t size() const override;
    std::unique_ptr<View> create_view(const std::vector<size_t> &dims) const override;
};

/**
 * A tensor implementation using serialized tensor addresses to
 * improve CPU cache and TLB hit ratio, relative to SimpleTensor
 * implementation.
 */
template<typename T>
class SparseTensorValue : public vespalib::eval::Value
{
private:
    eval::ValueType _type;
    SparseTensorValueIndex _index;
    ConstArrayRef<T> _cells;
    Stash _stash;
public:
    SparseTensorValue(const eval::ValueType &type_in, const SparseTensorValueIndex &index_in, ConstArrayRef<T> cells_in);

    SparseTensorValue(eval::ValueType &&type_in, SparseTensorValueIndex &&index_in, ConstArrayRef<T> &&cells_in, Stash &&stash_in);

    ~SparseTensorValue() override;

    TypedCells cells() const override { return TypedCells(_cells); }

    const Index &index() const override { return _index; }

    const eval::ValueType &type() const override { return _type; }
};

} // namespace
