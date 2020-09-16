

#include <vespa/eval/eval/value.h>
#include <vespa/eval/eval/value_type.h>
#include <vespa/eval/eval/simple_value.h>

namespace vespalib::eval {

struct PackedMixedFactory : ValueBuilderFactory {
    ~PackedMixedFactory() override {}
protected:
    std::unique_ptr<ValueBuilderBase> create_value_builder_base(const ValueType &type,
            size_t num_mapped_in, size_t subspace_size_in, size_t expect_subspaces) const override;
};

} // namespace
