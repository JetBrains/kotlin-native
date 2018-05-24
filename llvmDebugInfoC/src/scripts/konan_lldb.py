#!/usr/bin/python

##
# Copyright 2010-2017 JetBrains s.r.o.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#
# (lldb) command script import llvmDebugInfoC/src/scripts/konan_lldb.py
# (lldb) p kotlin_variable
#

import lldb
import struct

NULL = 'null'


def lldb_val_to_ptr(lldb_val):
    addr = lldb_val.GetValueAsUnsigned()
    return '((struct ObjHeader *) {:#x})'.format(addr)


def evaluate(expr):
    return lldb.debugger.GetSelectedTarget().EvaluateExpression(expr, lldb.SBExpressionOptions())


def is_instance_of(addr, typeinfo):
    return evaluate("(bool)IsInstance({}, {})".format(addr, typeinfo)).GetValue() == "true"


def is_string(value):
    return is_instance_of(lldb_val_to_ptr(value), "theStringTypeInfo")


def is_array(value):
    return int(evaluate("(int)Konan_DebugIsArray({})".format(lldb_val_to_ptr(value))).GetValue()) == 1

#
# Some kind of forward declaration.


__FACTORY = {}


def kotlin_object_type_summary(lldb_val, internal_dict):
    """Hook that is run by lldb to display a Kotlin object."""
    fallback = lldb_val.GetValue()
    if str(lldb_val.type) != "struct ObjHeader *":
        return fallback

    if int(fallback, 0) == 0:
        return NULL

    ptr = lldb_val_to_ptr(lldb_val)
    if ptr is None:
        return fallback

    return select_provider(lldb_val).to_string()


def select_provider(lldb_val):
    return __FACTORY['string'](lldb_val) if is_string(lldb_val) else __FACTORY['array'](lldb_val) if is_array(
    lldb_val) else __FACTORY['object'](lldb_val)


class KonanHelperProvider(lldb.SBSyntheticValueProvider):
    def __init__(self, valobj):
        self._target = lldb.debugger.GetSelectedTarget()
        self._process = self._target.GetProcess()
        self._valobj = valobj
        self._ptr = lldb_val_to_ptr(self._valobj)
        if is_string(valobj):
            return
        self._children_count = 0
        self._children = []
        self._type_conversion = {0: lambda address, _: "<invalid>{:#x}".format(address),
                                 1: lambda address, _: kotlin_object_type_summary(evaluate("(*(struct ObjHeader **){:#x})".format(address)), {}),
                                 2: lambda address, error: self.__read_memory(address, "<c", 1, error),
                                 3: lambda address, error: self.__read_memory(address, "<h", 2, error),
                                 4: lambda address, error: self.__read_memory(address, "<i", 4, error),
                                 5: lambda address, error: self.__read_memory(address, "<q", 8, error),
                                 6: lambda address, error: self.__read_memory(address, "<f", 4, error),
                                 7: lambda address, error: self.__read_memory(address, "<d", 8, error),
                                 8: lambda address, _: "(void *){:#x}".format(address),
                                 9: lambda address, error: self.__read_memory(address, "<?", 4, error)}

    def update(self):
        self._children_count = int(evaluate("(int)Konan_DebugGetFieldCount({})".format(self._ptr)).GetValue())

    def _read_string(self, expr, error):
        return self._process.ReadCStringFromMemory(int(evaluate(expr).GetValue(), 0), 0x1000, error)

    def _read_value(self, index, error):
        value_type = evaluate("(int)Konan_DebugGetFieldType({}, {})".format(self._ptr, index)).GetValue()
        address = int(evaluate("(void *)Konan_DebugGetFieldAddress({}, {})".format(self._ptr, index)).GetValue(), 0)
        return self._type_conversion[int(value_type)](address, error)

    def __read_memory(self, address, fmt, size, error):
        content = self._process.ReadMemory(address, size, error)
        return struct.unpack(fmt, content)[0] if error.Success() else "error: {:#x}".format(address)


class KonanStringSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj):
        super(KonanStringSyntheticProvider, self).__init__(valobj)
        fallback = valobj.GetValue()
        buff_len = evaluate(
            '(int)Konan_DebugObjectToUtf8Array({}, (char *)Konan_DebugBuffer(), (int)Konan_DebugBufferSize());'.format(self._ptr)
        ).unsigned

        if not buff_len:
            self._representation = fallback
            return

        buff_addr = evaluate("(char *)Konan_DebugBuffer()").unsigned

        error = lldb.SBError()
        s = self._process.ReadCStringFromMemory(int(buff_addr), int(buff_len), error)
        self._representation = s if error.Success() else fallback

    def update(self):
        pass

    def num_children(self):
        return 0

    def has_children(self):
        return False

    def get_child_index(self, _):
        return None

    def get_child_at_index(self, _):
        return None

    def to_string(self):
        return self._representation


class KonanObjectSyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj):
        super(KonanObjectSyntheticProvider, self).__init__(valobj)
        self.update()
        pass

    def update(self):
        super(KonanObjectSyntheticProvider, self).update()
        error = lldb.SBError()
        self._children = [
            self._read_string("(const char *)Konan_DebugGetFieldName({}, (int){})".format(self._ptr, i), error) for i in
            range(0, self._children_count) if error.Success()]

    def num_children(self):
        return self._children_count

    def has_children(self):
        return self._children_count > 0

    def get_child_index(self, name):
        return self._children.index(name)

    def get_child_at_index(self, index):
        return evaluate("(void *)Konan_DebugGetFieldAddress({}, {})".format(self._ptr, index)).GetValue()

    def to_string(self):
        error = lldb.SBError()
        return dict([(self._children[i], self._read_value(i, error)) for i in range(0, self._children_count)])



class KonanArraySyntheticProvider(KonanHelperProvider):
    def __init__(self, valobj):
        super(KonanArraySyntheticProvider, self).__init__(valobj)
        if self._ptr is None:
            return
        self.update()

    def update(self):
        super(KonanArraySyntheticProvider, self).update()
        self._children = [x for x in range(0, self.num_children())]
        pass

    def num_children(self):
        return self._children_count

    def has_children(self):
        return self._children_count > 0

    def get_child_index(self, name):
        index = int(name)
        return index if (0 <= index < self._num_children) else None

    def get_child_at_index(self, index):
        error = lldb.SBError()
        return self._read_value(index, error) if error.Success() else None

    def to_string(self):
        error = lldb.SBError()
        return [self._read_value(i, error) for i in range(0, self._children_count)]


class KonanProxyTypeProvider:
    def __init__(self, valobj, _):
        self._proxy = select_provider(valobj)
        self.update()

    def __getattr__(self, item):
        return getattr(self._proxy, item)


def __lldb_init_module(debugger, _):
    __FACTORY['object'] = lambda x: KonanObjectSyntheticProvider(x)
    __FACTORY['array'] = lambda x: KonanArraySyntheticProvider(x)
    __FACTORY['string'] = lambda x: KonanStringSyntheticProvider(x)
    debugger.HandleCommand('\
        type summary add \
        --no-value \
        --expand \
        --python-function konan_lldb.kotlin_object_type_summary \
        "ObjHeader *" \
        --category Kotlin\
    ')
    debugger.HandleCommand('\
        type synthetic add \
        --python-class konan_lldb.KonanProxyTypeProvider\
        "ObjHeader *" \
        --category Kotlin\
    ')
    debugger.HandleCommand('type category enable Kotlin')
