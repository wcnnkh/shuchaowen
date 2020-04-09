/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scw.cglib.core;

import scw.asm.Type;

/**
 * @author Juozas Baliuka <a href="mailto:baliuka@mwm.lt">baliuka@mwm.lt</a>
 * @version $Id: Constants.java,v 1.21 2006/03/05 02:43:19 herbyderby Exp $
 */
@SuppressWarnings({"rawtypes"})
public interface CGLIBConstants extends scw.asm.Opcodes {

    /* Indicates the ASM API version that is used throughout cglib */
    public static final int ASM_API = scw.core.Constants.ASM_VERSION;

    public static final Class[] EMPTY_CLASS_ARRAY = {};
    public static final Type[] TYPES_EMPTY = {};

    public static final Signature SIG_STATIC =
      CGLIBTypeUtils.parseSignature("void <clinit>()");
    
    public static final Type TYPE_OBJECT_ARRAY = CGLIBTypeUtils.parseType("Object[]");
    public static final Type TYPE_CLASS_ARRAY = CGLIBTypeUtils.parseType("Class[]");
    public static final Type TYPE_STRING_ARRAY = CGLIBTypeUtils.parseType("String[]");

    public static final Type TYPE_OBJECT = CGLIBTypeUtils.parseType("Object");
    public static final Type TYPE_CLASS = CGLIBTypeUtils.parseType("Class");
    public static final Type TYPE_CLASS_LOADER = CGLIBTypeUtils.parseType("ClassLoader");
    public static final Type TYPE_CHARACTER = CGLIBTypeUtils.parseType("Character");
    public static final Type TYPE_BOOLEAN = CGLIBTypeUtils.parseType("Boolean");
    public static final Type TYPE_DOUBLE = CGLIBTypeUtils.parseType("Double");
    public static final Type TYPE_FLOAT = CGLIBTypeUtils.parseType("Float");
    public static final Type TYPE_LONG = CGLIBTypeUtils.parseType("Long");
    public static final Type TYPE_INTEGER = CGLIBTypeUtils.parseType("Integer");
    public static final Type TYPE_SHORT = CGLIBTypeUtils.parseType("Short");
    public static final Type TYPE_BYTE = CGLIBTypeUtils.parseType("Byte");
    public static final Type TYPE_NUMBER = CGLIBTypeUtils.parseType("Number");
    public static final Type TYPE_STRING = CGLIBTypeUtils.parseType("String");
    public static final Type TYPE_THROWABLE = CGLIBTypeUtils.parseType("Throwable");
    public static final Type TYPE_BIG_INTEGER = CGLIBTypeUtils.parseType("java.math.BigInteger");
    public static final Type TYPE_BIG_DECIMAL = CGLIBTypeUtils.parseType("java.math.BigDecimal");
    public static final Type TYPE_STRING_BUFFER = CGLIBTypeUtils.parseType("StringBuffer");
    public static final Type TYPE_RUNTIME_EXCEPTION = CGLIBTypeUtils.parseType("RuntimeException");
    public static final Type TYPE_ERROR = CGLIBTypeUtils.parseType("Error");
    public static final Type TYPE_SYSTEM = CGLIBTypeUtils.parseType("System");
    public static final Type TYPE_SIGNATURE = CGLIBTypeUtils.parseType(Signature.class.getName());
    public static final Type TYPE_TYPE = Type.getType(Type.class);

    public static final String CONSTRUCTOR_NAME = "<init>";
    public static final String STATIC_NAME = "<clinit>";
    public static final String SOURCE_FILE = "<generated>";
    public static final String SUID_FIELD_NAME = "serialVersionUID";

    public static final int PRIVATE_FINAL_STATIC = ACC_PRIVATE | ACC_FINAL | ACC_STATIC;

    public static final int SWITCH_STYLE_TRIE = 0;
    public static final int SWITCH_STYLE_HASH = 1;
    public static final int SWITCH_STYLE_HASHONLY = 2;
}