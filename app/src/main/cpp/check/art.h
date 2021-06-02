//
// Created by Thom on 2019-09-15.
//

#ifndef BREVENT_ART_H
#define BREVENT_ART_H

#include <stdint.h>

#ifndef NDEBUG
#define ALWAYS_INLINE
#else
#define ALWAYS_INLINE  __attribute__ ((always_inline))
#endif

#define DCHECK(...)

#define OVERRIDE override

#define ATTRIBUTE_UNUSED __attribute__((__unused__))

#define REQUIRES_SHARED(...)

#define MANAGED PACKED(4)
#define PACKED(x) __attribute__ ((__aligned__(x), __packed__))

namespace art {

    namespace mirror {

        class Object {

        };

        template<class MirrorType>
        class ObjPtr {

        public:
            MirrorType *Ptr() const {
                return nullptr;
            }

        };

        template<bool kPoisonReferences, class MirrorType>
        class PtrCompression {
        public:
            // Compress reference to its bit representation.
            static uint32_t Compress(MirrorType *mirror_ptr) {
                uintptr_t as_bits = reinterpret_cast<uintptr_t>(mirror_ptr);
                return static_cast<uint32_t>(kPoisonReferences ? -as_bits : as_bits);
            }

            // Uncompress an encoded reference from its bit representation.
            static MirrorType *Decompress(uint32_t ref) {
                uintptr_t as_bits = kPoisonReferences ? -ref : ref;
                return reinterpret_cast<MirrorType *>(as_bits);
            }

            // Convert an ObjPtr to a compressed reference.
            static uint32_t Compress(ObjPtr<MirrorType> ptr) REQUIRES_SHARED(Locks::mutator_lock_) {
                return Compress(ptr.Ptr());
            }
        };


        // Value type representing a reference to a mirror::Object of type MirrorType.
        template<bool kPoisonReferences, class MirrorType>
        class MANAGED ObjectReference {
        private:
            using Compression = PtrCompression<kPoisonReferences, MirrorType>;

        public:
            MirrorType *AsMirrorPtr() const {
                return Compression::Decompress(reference_);
            }

            void Assign(MirrorType *other) {
                reference_ = Compression::Compress(other);
            }

            void Assign(ObjPtr<MirrorType> ptr) REQUIRES_SHARED(Locks::mutator_lock_);

            void Clear() {
                reference_ = 0;
                DCHECK(IsNull());
            }

            bool IsNull() const {
                return reference_ == 0;
            }

            uint32_t AsVRegValue() const {
                return reference_;
            }

            static ObjectReference<kPoisonReferences, MirrorType>
            FromMirrorPtr(MirrorType *mirror_ptr)
            REQUIRES_SHARED(Locks::mutator_lock_) {
                return ObjectReference<kPoisonReferences, MirrorType>(mirror_ptr);
            }

        protected:
            explicit ObjectReference(MirrorType *mirror_ptr) REQUIRES_SHARED(Locks::mutator_lock_)
                    : reference_(Compression::Compress(mirror_ptr)) {
            }

            // The encoded reference to a mirror::Object.
            uint32_t reference_;
        };

        // Standard compressed reference used in the runtime. Used for StackReference and GC roots.
        template<class MirrorType>
        class MANAGED CompressedReference : public mirror::ObjectReference<false, MirrorType> {
        public:
            CompressedReference<MirrorType>() REQUIRES_SHARED(Locks::mutator_lock_)
                    : mirror::ObjectReference<false, MirrorType>(nullptr) {}

            static CompressedReference<MirrorType> FromMirrorPtr(MirrorType *p)
            REQUIRES_SHARED(Locks::mutator_lock_) {
                return CompressedReference<MirrorType>(p);
            }

        private:
            explicit CompressedReference(MirrorType *p) REQUIRES_SHARED(Locks::mutator_lock_)
                    : mirror::ObjectReference<false, MirrorType>(p) {}
        };
    }

    class RootInfo {

    };

    class RootVisitor {
    public:
        virtual ~RootVisitor() {}

        // Single root version, not overridable.
        ALWAYS_INLINE void VisitRoot(mirror::Object **root, const RootInfo &info)
        REQUIRES_SHARED(Locks::mutator_lock_) {
            VisitRoots(&root, 1, info);
        }

        // Single root version, not overridable.
        ALWAYS_INLINE void VisitRootIfNonNull(mirror::Object **root, const RootInfo &info)
        REQUIRES_SHARED(Locks::mutator_lock_) {
            if (*root != nullptr) {
                VisitRoot(root, info);
            }
        }

        virtual void VisitRoots(mirror::Object ***roots, size_t count, const RootInfo &info)
        REQUIRES_SHARED(Locks::mutator_lock_) = 0;

        virtual void VisitRoots(mirror::CompressedReference<mirror::Object> **roots, size_t count,
                                const RootInfo &info)
        REQUIRES_SHARED(Locks::mutator_lock_) = 0;
    };

    // Only visits roots one at a time, doesn't handle updating roots. Used when performance isn't
    // critical.
    class SingleRootVisitor : public RootVisitor {
    private:
        void VisitRoots(mirror::Object ***roots, size_t count, const RootInfo &info) OVERRIDE
        REQUIRES_SHARED(Locks::mutator_lock_) {
            for (size_t i = 0; i < count; ++i) {
                VisitRoot(*roots[i], info);
            }
        }

        void VisitRoots(mirror::CompressedReference<mirror::Object> **roots, size_t count,
                        const RootInfo &info) OVERRIDE
        REQUIRES_SHARED(Locks::mutator_lock_) {
            for (size_t i = 0; i < count; ++i) {
                VisitRoot(roots[i]->AsMirrorPtr(), info);
            }
        }

        virtual void VisitRoot(mirror::Object *root, const RootInfo &info) = 0;
    };

    class IsMarkedVisitor {
    public:
        virtual ~IsMarkedVisitor() {}

        // Return null if an object is not marked, otherwise returns the new address of that object.
        // May return the same address as the input if the object did not move.
        virtual mirror::Object *IsMarked(mirror::Object *obj) = 0;
    };

}

#endif //BREVENT_ART_H
