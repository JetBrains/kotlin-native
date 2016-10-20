#ifndef RUNTIME_MEMORY_H
#define RUNTIME_MEMORY_H

#include "Assert.h"
#include "TypeInfo.h"

typedef enum {
  // Allocation guaranteed to be frame local.
  SCOPE_FRAME = 0,
  // Allocation is generic global allocation.
  SCOPE_GLOBAL = 1,
  // Allocation shall take place in current arena.
  SCOPE_ARENA = 2
} PlacementHint;

// Must fit in two bits.
typedef enum {
  // Container is normal thread local container.
  CONTAINER_TAG_NORMAL = 0,
  // Container shall not be refcounted (const data, frame locals).
  CONTAINER_TAG_NOCOUNT = 1,
  // Container shall be atomically refcounted.
  CONTAINER_TAG_SHARED = 2,
  // Container is no longer valid.
  CONTAINER_TAG_INVALID = 3,
  // Actual value to increment/decrement conatiner by. Tag is in lower bits.
  CONTAINER_TAG_INCREMENT = 1 << 2,
  // Mask for container type.
  CONTAINER_TAG_MASK = (CONTAINER_TAG_INCREMENT - 1)
} ContainerTag;

// Could be made 64-bit for large memory configs.
typedef uint32_t container_offset_t;

// Header of every object.
struct ObjHeader {
  const TypeInfo* type_info_;
  container_offset_t container_offset_negative_;
};

// Header of value type array objects.
struct ArrayHeader : public ObjHeader {
  // Elements count. Element size is stored in instanceSize_ field of TypeInfo, negated.
  uint32_t count_;
};

// Header of all container objects. Contains reference counter.
struct ContainerHeader {
  // Reference counter of container. Uses two lower bits of counter for
  // container type (for polymorphism in ::Release()).
  volatile uint32_t ref_count_;
};

struct ArenaContainerHeader : public ContainerHeader {
  // Current allocation limit.
  uint8_t* current_;
  // Allocation end. Maybe consider having chunked backing storage
  // at cost of smarter ::Release() polymorphic on container type.
  uint8_t* end_;
};

inline void* AddressOfElementAt(ArrayHeader* obj, int32_t index) {
  // Instance size is negative.
  return reinterpret_cast<uint8_t*>(obj + 1) - obj->type_info_->instanceSize_ * index;
}

// Optimized version not accessing type info.
inline uint8_t* ByteArrayAddressOfElementAt(ArrayHeader* obj, int32_t index) {
  return reinterpret_cast<uint8_t*>(obj + 1) + index;
}

inline const uint8_t* ByteArrayAddressOfElementAt(const ArrayHeader* obj, int32_t index) {
  return reinterpret_cast<const uint8_t*>(obj + 1) + index;
}

inline uint32_t ArraySizeBytes(const ArrayHeader* obj) {
  // Instance size is negative.
  return -obj->type_info_->instanceSize_ * obj->count_;
}


// Those two operations are implemented by translator when storing references
// to objects.
inline void AddRef(ContainerHeader* header) {
  // Looking at container type we may want to skip AddRef() totally
  // (non-escaping stack objects, constant objects).
  switch (header->ref_count_ & CONTAINER_TAG_MASK) {
    case CONTAINER_TAG_NORMAL:
      header->ref_count_ += CONTAINER_TAG_INCREMENT;
      break;
    case CONTAINER_TAG_NOCOUNT:
      break;
    case CONTAINER_TAG_SHARED:
      __sync_fetch_and_add(&header->ref_count_, CONTAINER_TAG_INCREMENT);
      break;
    case CONTAINER_TAG_INVALID:
      RuntimeAssert(false, "trying to addref invalid container");
      break;
  }
}

void FreeObject(ContainerHeader* header);

inline void Release(ContainerHeader* header) {
  switch (header->ref_count_ & CONTAINER_TAG_MASK) {
    case CONTAINER_TAG_NORMAL:
      if ((header->ref_count_ -= CONTAINER_TAG_INCREMENT) == CONTAINER_TAG_NORMAL) {
        FreeObject(header);
      }
      break;
    case CONTAINER_TAG_NOCOUNT:
      break;
    // Note that shared containers have potentially subtle race, if object holds a
    // reference to another object, stored in shorter living container. In this
    // case there's unlikely, but possible case, where one mutator takes reference,
    // from the field, but not yet AddRef'ed it, while another mutator updates
    // field with another value, and thus Release's same field.
    // If those two updates happens concurrently - it may lead to dereference of stale
    // pointer.
    // It seems not a very big issue as:
    //  - if objects stored in the same container, race will never happen
    //  - if concurrent field access is under lock, race will never happen
    //  - container likely groups multiple objects, so object release will lead to
    //    container release only in relatively few cases, which will decrease race
    //    probability even further.
    case CONTAINER_TAG_SHARED:
      if (__sync_sub_and_fetch(
              &header->ref_count_, CONTAINER_TAG_INCREMENT) == CONTAINER_TAG_SHARED) {
        FreeObject(header);
      }
      break;
    case CONTAINER_TAG_INVALID:
      RuntimeAssert(false, "trying to release invalid container");
      break;
  }
}

// Class representing arbitrary placement container.
class Container {
 protected:
  // Data where everything is being stored.
  ContainerHeader* header_;

  void SetMeta(ObjHeader* obj, const TypeInfo* type_info) {
    obj->container_offset_negative_ =
        reinterpret_cast<uintptr_t>(obj) - reinterpret_cast<uintptr_t>(header_);
    obj->type_info_ = type_info;
  }

 public:
  // Increment reference counter associated with container.
  void AddRef() {
    if (header_) ::AddRef(header_);
  }

  // Decrement reference counter associated with container.
  // For objects whith tricky lifetime (such as ones shared between threads objects)
  // individual container per object (ObjectContainer) shall be created.
  // As an alternative, such objects could be evacuated from short-lived containers.
  void Release() {
    if (header_) ::Release(header_);
  }
};

// Container for a single object.
class ObjectContainer : public Container {
 public:
  // Single instance.
  explicit ObjectContainer(const TypeInfo* type_info) {
    Init(type_info);
  }

  // Object container shalln't have any dtor, as it's being freed by ::Release().
  ObjHeader* GetPlace() const {
    return reinterpret_cast<ObjHeader*>(
        reinterpret_cast<uint8_t*>(header_) + sizeof(ContainerHeader));
  }

 private:
  void Init(const TypeInfo* type_info);
};


class ArrayContainer : public Container {
 public:
  ArrayContainer(const TypeInfo* type_info, uint32_t elements) {
    Init(type_info, elements);
  }

  // Array container shalln't have any dtor, as it's being freed by ::Release().
  ArrayHeader* GetPlace() const {
    return reinterpret_cast<ArrayHeader*>(
        reinterpret_cast<uint8_t*>(header_) + sizeof(ContainerHeader));
  }

 private:
  void Init(const TypeInfo* type_info, uint32_t elements);
};


// Class representing arena-style placement container.
// Container is used for reference counting,
// and it is assumed that objects with related placement will share container. Only
// whole container can be freed, individual objects are not taken into account.
class ArenaContainer : public Container {
 public:
  explicit ArenaContainer(uint32_t size);

  ~ArenaContainer() {
    if (header_) {
      RuntimeAssert(header_->ref_count_ == 0, "Non-zero refcount");
      Dispose();
    }
  }

  // Allocation function.
  void* Place(int size) {
    ArenaContainerHeader* header = reinterpret_cast<ArenaContainerHeader*>(header_);
    if (header->current_ + size > header->end_) {
      return nullptr;
    }
    void* result = header->current_;
    header->current_ += size;
    return result;
  }

  // Place individual object in this container.
  ObjHeader* PlaceObject(const TypeInfo* type_info);

  // Places an array of certain type in this container. Note that array_type_info
  // is type info for an array, not for an individual element. Also note that exactly
  // same operation could be used to place strings.
  ArrayHeader* PlaceArray(const TypeInfo* array_type_info, int count);

  // Dispose whole container ignoring non-zero refcount. Use with care.
  void Dispose() {
    if (header_) {
      FreeObject(header_);
      header_ = nullptr;
    }
  }
};

#ifdef __cplusplus
extern "C" {
#endif

void InitMemory();
void* AllocInstance(const TypeInfo* type_info, PlacementHint hint);
void* AllocArrayInstance(const TypeInfo* type_info, PlacementHint hint, uint32_t elements);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_MEMORY_H
