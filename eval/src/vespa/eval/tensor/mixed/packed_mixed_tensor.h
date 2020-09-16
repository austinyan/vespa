// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#pragma once

#include <vespa/eval/eval/value.h>
#include <vespa/eval/eval/value_type.h>
#include <vespa/eval/eval/simple_value.h>

#include <vespa/eval/tensor/sparse/packed_mappings.h>
#include <vespa/eval/tensor/sparse/packed_mappings_builder.h>

namespace vespalib::eval {

class PackedMixedTensor : public NewValue, public NewValue::Index
{
private:
    const ValueType _type;
    const TypedCells _cells;
    const PackedMappings _mappings;

    PackedMixedTensor(const ValueType &type, TypedCells cells,
                      PackedMappings mappings)
      : _type(type),
        _cells(cells),
        _mappings(mappings)
    {}

    template<typename T> friend class PackedMixedBuilder;

public:
    ~PackedMixedTensor() override;

    // NewValue API:
    const ValueType &type() const override { return _type; }
    const NewValue::Index &index() const override { return *this; }
    TypedCells cells() const override { return _cells; }

    // NewValue::Index API:
    size_t size() const override { return _mappings.size(); }
    std::unique_ptr<View> create_view(const std::vector<size_t> &dims) const override;
};

} // namespace
