// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

#include "packed_mixed_builder.h"

namespace vespalib::eval {

template <typename T>
ArrayRef<T> 
PackedMixedBuilder<T>::add_subspace(const std::vector<vespalib::stringref> &addr)
{
    size_t old_size = _cells.size();
    uint32_t idx = _mappings_builder.add_mapping_for(addr);
    assert((idx * _subspace_size) == old_size);
    _cells.resize(old_size + _subspace_size);
    return ArrayRef<T>(&_cells[old_size], _subspace_size);
}


template <typename T>
std::unique_ptr<NewValue>
PackedMixedBuilder<T>::build(std::unique_ptr<ValueBuilder<T>> self)
{
    size_t meta_size = sizeof(PackedMixedTensor);
    size_t mappings_size = _mappings_builder.extra_memory();
    // align:
    mappings_size += 15ul;
    mappings_size &= ~15ul;
    size_t cells_size = sizeof(T) * _cells.size();
    size_t total_size = sizeof(PackedMixedTensor) + mappings_size + cells_size;

    char *mem = (char *) operator new(total_size);
    char *mappings_mem = mem + meta_size;
    char *cells_mem = mappings_mem + mappings_size;

    // fill mapping data:
    auto mappings = _mappings_builder.target_memory(mappings_mem, cells_mem);

    // copy cells:
    memcpy(cells_mem, &_cells[0], cells_size);
    ConstArrayRef<T> cells((T *)cells_mem, _cells.size());

    PackedMixedTensor * built =
        new (mem) PackedMixedTensor(_type, TypedCells(cells), mappings);

    // keep "this" alive until this point:
    (void) self;
    return std::unique_ptr<PackedMixedTensor>(built);
}

template class PackedMixedBuilder<float>;
template class PackedMixedBuilder<double>;

} // namespace
