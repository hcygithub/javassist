/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2006 Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */

package javassist;

import javassist.bytecode.*;
import javassist.compiler.Javac;
import javassist.compiler.CompileError;
import javassist.expr.ExprEditor;

/**
 * <code>CtBehavior</code> represents a method, a constructor,
 * or a static constructor (class initializer). 
 * It is the abstract super class of
 * <code>CtMethod</code> and <code>CtConstructor</code>.
 */
public abstract class CtBehavior extends CtMember {
    protected MethodInfo methodInfo;

    protected CtBehavior(CtClass clazz, MethodInfo minfo) {
        super(clazz);
        methodInfo = minfo;
    }

    /**
     * @param isCons        true if this is a constructor.
     */
    void copy(CtBehavior src, boolean isCons, ClassMap map)
        throws CannotCompileException
    {
        CtClass declaring = declaringClass;
        MethodInfo srcInfo = src.methodInfo;
        CtClass srcClass = src.getDeclaringClass();
        ConstPool cp = declaring.getClassFile2().getConstPool();

        map = new ClassMap(map);
        map.put(srcClass.getName(), declaring.getName());
        try {
            boolean patch = false;
            CtClass srcSuper = srcClass.getSuperclass();
            CtClass destSuper = declaring.getSuperclass();
            String destSuperName = null;
            if (srcSuper != null && destSuper != null) {
                String srcSuperName = srcSuper.getName();
                destSuperName = destSuper.getName();
                if (!srcSuperName.equals(destSuperName))
                    if (srcSuperName.equals(CtClass.javaLangObject))
                        patch = true;
                    else
                        map.put(srcSuperName, destSuperName);
            }

            methodInfo = new MethodInfo(cp, srcInfo.getName(), srcInfo, map);
            if (isCons && patch)
                methodInfo.setSuperclass(destSuperName);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    protected void extendToString(StringBuffer buffer) {
        buffer.append(' ');
        buffer.append(getName());
        buffer.append(' ');
        buffer.append(methodInfo.getDescriptor());
    }

    /**
     * Returns the method or constructor name followed by parameter types
     * such as <code>javassist.CtBehavior.stBody(String)</code>.
     *
     * @since 3.5
     */
    public abstract String getLongName();

    /**
     * Returns the MethodInfo representing this method/constructor in the
     * class file.
     */
    public MethodInfo getMethodInfo() {
        declaringClass.checkModify();
        return methodInfo;
    }

    /**
     * Returns the MethodInfo representing the method/constructor in the
     * class file (read only).
     * Normal applications do not need calling this method.  Use
     * <code>getMethodInfo()</code>.
     *
     * <p>The <code>MethodInfo</code> object obtained by this method
     * is read only.  Changes to this object might not be reflected
     * on a class file generated by <code>toBytecode()</code>,
     * <code>toClass()</code>, etc in <code>CtClass</code>.
     *
     * <p>This method is available even if the <code>CtClass</code>
     * containing this method is frozen.  However, if the class is
     * frozen, the <code>MethodInfo</code> might be also pruned.
     *
     * @see #getMethodInfo()
     * @see CtClass#isFrozen()
     * @see CtClass#prune()
     */
    public MethodInfo getMethodInfo2() { return methodInfo; }

    /**
     * Obtains the modifiers of the method/constructor.
     *
     * @return          modifiers encoded with
     *                  <code>javassist.Modifier</code>.
     * @see Modifier
     */
    public int getModifiers() {
        return AccessFlag.toModifier(methodInfo.getAccessFlags());
    }

    /**
     * Sets the encoded modifiers of the method/constructor.
     *
     * <p>Changing the modifiers may cause a problem.
     * For example, if a non-static method is changed to static,
     * the method will be rejected by the bytecode verifier.
     *
     * @see Modifier
     */
    public void setModifiers(int mod) {
        declaringClass.checkModify();
        methodInfo.setAccessFlags(AccessFlag.of(mod));
    }

    /**
     * Returns the annotations associated with this method or constructor.
     *
     * @return an array of annotation-type objects.
     * @see #getAvailableAnnotations()
     * @since 3.1
     */
    public Object[] getAnnotations() throws ClassNotFoundException {
       return getAnnotations(false);
   }

    /**
     * Returns the annotations associated with this method or constructor.
     * If any annotations are not on the classpath, they are not included
     * in the returned array.
     * 
     * @return an array of annotation-type objects.
     * @see #getAnnotations()
     * @since 3.3
     */
    public Object[] getAvailableAnnotations(){
       try{
           return getAnnotations(true);
       }
       catch (ClassNotFoundException e){
           throw new RuntimeException("Unexpected exception", e);
       }
    }

    private Object[] getAnnotations(boolean ignoreNotFound)
       throws ClassNotFoundException
    {
       MethodInfo mi = getMethodInfo2();
       AnnotationsAttribute ainfo = (AnnotationsAttribute)
                   mi.getAttribute(AnnotationsAttribute.invisibleTag);  
       AnnotationsAttribute ainfo2 = (AnnotationsAttribute)
                   mi.getAttribute(AnnotationsAttribute.visibleTag);  
       return CtClassType.toAnnotationType(ignoreNotFound,
                                           getDeclaringClass().getClassPool(),
                                           ainfo, ainfo2);
    }

    /**
     * Returns the parameter annotations associated with this method or constructor.
     *
     * @return an array of annotation-type objects.  The length of the returned array is
     * equal to the number of the formal parameters.  If each parameter has no
     * annotation, the elements of the returned array are empty arrays.
     *
     * @see #getAvailableParameterAnnotations()
     * @see #getAnnotations()
     * @since 3.1
     */
    public Object[][] getParameterAnnotations() throws ClassNotFoundException {
        return getParameterAnnotations(false);
    }

    /**
     * Returns the parameter annotations associated with this method or constructor.
     * If any annotations are not on the classpath, they are not included in the
     * returned array.
     * 
     * @return an array of annotation-type objects.  The length of the returned array is
     * equal to the number of the formal parameters.  If each parameter has no
     * annotation, the elements of the returned array are empty arrays.
     *
     * @see #getParameterAnnotations()
     * @see #getAvailableAnnotations()
     * @since 3.3
     */
    public Object[][] getAvailableParameterAnnotations(){
        try {
            return getParameterAnnotations(true);
        }
        catch(ClassNotFoundException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    Object[][] getParameterAnnotations(boolean ignoreNotFound)
        throws ClassNotFoundException
    {
        MethodInfo mi = getMethodInfo2();
        ParameterAnnotationsAttribute ainfo = (ParameterAnnotationsAttribute)
                    mi.getAttribute(ParameterAnnotationsAttribute.invisibleTag);  
        ParameterAnnotationsAttribute ainfo2 = (ParameterAnnotationsAttribute)
                    mi.getAttribute(ParameterAnnotationsAttribute.visibleTag);  
        return CtClassType.toAnnotationType(ignoreNotFound,
                                            getDeclaringClass().getClassPool(),
                                            ainfo, ainfo2, mi);
    }

    /**
     * Obtains parameter types of this method/constructor.
     */
    public CtClass[] getParameterTypes() throws NotFoundException {
        return Descriptor.getParameterTypes(methodInfo.getDescriptor(),
                                            declaringClass.getClassPool());
    }

    /**
     * Obtains the type of the returned value.
     */
    CtClass getReturnType0() throws NotFoundException {
        return Descriptor.getReturnType(methodInfo.getDescriptor(),
                                        declaringClass.getClassPool());
    }

    /**
     * Returns the method signature (the parameter types
     * and the return type).
     * The method signature is represented by a character string
     * called method descriptor, which is defined in the JVM specification.
     * If two methods/constructors have
     * the same parameter types
     * and the return type, <code>getSignature()</code> returns the
     * same string (the return type of constructors is <code>void</code>).
     *
     * <p>Note that the returned string is not the type signature
     * contained in the <code>SignatureAttirbute</code>.  It is
     * a descriptor.  To obtain a type signature, call the following
     * methods:
     * 
     * <ul><pre>getMethodInfo().getAttribute(SignatureAttribute.tag)
     * </pre></ul>
     *
     * @see javassist.bytecode.Descriptor
     * @see javassist.bytecode.SignatureAttribute
     */
    public String getSignature() {
        return methodInfo.getDescriptor();
    }

    /**
     * Obtains exceptions that this method/constructor may throw.
     *
     * @return a zero-length array if there is no throws clause.
     */
    public CtClass[] getExceptionTypes() throws NotFoundException {
        String[] exceptions;
        ExceptionsAttribute ea = methodInfo.getExceptionsAttribute();
        if (ea == null)
            exceptions = null;
        else
            exceptions = ea.getExceptions();

        return declaringClass.getClassPool().get(exceptions);
    }

    /**
     * Sets exceptions that this method/constructor may throw.
     */
    public void setExceptionTypes(CtClass[] types) throws NotFoundException {
        declaringClass.checkModify();
        if (types == null || types.length == 0) {
            methodInfo.removeExceptionsAttribute();
            return;
        }

        String[] names = new String[types.length];
        for (int i = 0; i < types.length; ++i)
            names[i] = types[i].getName();

        ExceptionsAttribute ea = methodInfo.getExceptionsAttribute();
        if (ea == null) {
            ea = new ExceptionsAttribute(methodInfo.getConstPool());
            methodInfo.setExceptionsAttribute(ea);
        }

        ea.setExceptions(names);
    }

    /**
     * Returns true if the body is empty.
     */
    public abstract boolean isEmpty();

    /**
     * Sets a method/constructor body.
     *
     * @param src       the source code representing the body.
     *                  It must be a single statement or block.
     *                  If it is <code>null</code>, the substituted
     *                  body does nothing except returning zero or null.
     */
    public void setBody(String src) throws CannotCompileException {
        setBody(src, null, null);
    }

    /**
     * Sets a method/constructor body.
     *
     * @param src       the source code representing the body.
     *                  It must be a single statement or block.
     *                  If it is <code>null</code>, the substituted
     *                  body does nothing except returning zero or null.
     * @param delegateObj       the source text specifying the object
     *                          that is called on by <code>$proceed()</code>.
     * @param delegateMethod    the name of the method
     *                          that is called by <code>$proceed()</code>.
     */
    public void setBody(String src,
                        String delegateObj, String delegateMethod)
        throws CannotCompileException
    {
        declaringClass.checkModify();
        try {
            Javac jv = new Javac(declaringClass);
            if (delegateMethod != null)
                jv.recordProceed(delegateObj, delegateMethod);

            Bytecode b = jv.compileBody(this, src);
            methodInfo.setCodeAttribute(b.toCodeAttribute());
            methodInfo.setAccessFlags(methodInfo.getAccessFlags()
                                      & ~AccessFlag.ABSTRACT);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
    }

    static void setBody0(CtClass srcClass, MethodInfo srcInfo,
                         CtClass destClass, MethodInfo destInfo,
                         ClassMap map)
        throws CannotCompileException
    {
        destClass.checkModify();

        map = new ClassMap(map);
        map.put(srcClass.getName(), destClass.getName());
        try {
            CodeAttribute cattr = srcInfo.getCodeAttribute();
            if (cattr != null) {
                ConstPool cp = destInfo.getConstPool();
                CodeAttribute ca = (CodeAttribute)cattr.copy(cp, map);
                destInfo.setCodeAttribute(ca);
            }
        }
        catch (CodeAttribute.RuntimeCopyException e) {
            /* the exception may be thrown by copy() in CodeAttribute.
             */
            throw new CannotCompileException(e);
        }

        destInfo.setAccessFlags(destInfo.getAccessFlags()
                                & ~AccessFlag.ABSTRACT);
    }

    /**
     * Obtains an attribute with the given name.
     * If that attribute is not found in the class file, this
     * method returns null.
     *
     * <p>Note that an attribute is a data block specified by
     * the class file format.  It is not an annotation.
     * See {@link javassist.bytecode.AttributeInfo}.
     *
     * @param name              attribute name
     */
    public byte[] getAttribute(String name) {
        AttributeInfo ai = methodInfo.getAttribute(name);
        if (ai == null)
            return null;
        else
            return ai.get();
    }

    /**
     * Adds an attribute. The attribute is saved in the class file.
     *
     * <p>Note that an attribute is a data block specified by
     * the class file format.  It is not an annotation.
     * See {@link javassist.bytecode.AttributeInfo}.
     *
     * @param name      attribute name
     * @param data      attribute value
     */
    public void setAttribute(String name, byte[] data) {
        declaringClass.checkModify();
        methodInfo.addAttribute(new AttributeInfo(methodInfo.getConstPool(),
                                                  name, data));
    }

    /**
     * Declares to use <code>$cflow</code> for this method/constructor.
     * If <code>$cflow</code> is used, the class files modified
     * with Javassist requires a support class
     * <code>javassist.runtime.Cflow</code> at runtime
     * (other Javassist classes are not required at runtime).
     *
     * <p>Every <code>$cflow</code> variable is given a unique name.
     * For example, if the given name is <code>"Point.paint"</code>,
     * then the variable is indicated by <code>$cflow(Point.paint)</code>.
     *
     * @param name      <code>$cflow</code> name.  It can include
     *                  alphabets, numbers, <code>_</code>,
     *                  <code>$</code>, and <code>.</code> (dot).
     *
     * @see javassist.runtime.Cflow
     */
    public void useCflow(String name) throws CannotCompileException {
        CtClass cc = declaringClass;
        cc.checkModify();
        ClassPool pool = cc.getClassPool();
        String fname;
        int i = 0;
        while (true) {
            fname = "_cflow$" + i++;
            try {
                cc.getDeclaredField(fname);
            }
            catch(NotFoundException e) {
                break;
            }
        }

        pool.recordCflow(name, declaringClass.getName(), fname);
        try {
            CtClass type = pool.get("javassist.runtime.Cflow");
            CtField field = new CtField(type, fname, cc);
            field.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
            cc.addField(field, CtField.Initializer.byNew(type));
            insertBefore(fname + ".enter();");
            String src = fname + ".exit();";
            insertAfter(src, true);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Declares a new local variable.  The scope of this variable is the
     * whole method body.  The initial value of that variable is not set.
     * The declared variable can be accessed in the code snippet inserted
     * by <code>insertBefore()</code>, <code>insertAfter()</code>, etc.
     *
     * <p>If the second parameter <code>asFinally</code> to
     * <code>insertAfter()</code> is true, the declared local variable
     * is not visible from the code inserted by <code>insertAfter()</code>.
     *
     * @param name      the name of the variable
     * @param type      the type of the variable
     * @see #insertBefore(String)
     * @see #insertAfter(String)
     */
    public void addLocalVariable(String name, CtClass type)
        throws CannotCompileException
    {
        declaringClass.checkModify();
        ConstPool cp = methodInfo.getConstPool();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        if (ca == null)
            throw new CannotCompileException("no method body");

        LocalVariableAttribute va = (LocalVariableAttribute)ca.getAttribute(
                                                LocalVariableAttribute.tag);
        if (va == null) {
            va = new LocalVariableAttribute(cp);
            ca.getAttributes().add(va);
        }

        int maxLocals = ca.getMaxLocals();
        String desc = Descriptor.of(type);
        va.addEntry(0, ca.getCodeLength(),
                    cp.addUtf8Info(name), cp.addUtf8Info(desc), maxLocals);
        ca.setMaxLocals(maxLocals + Descriptor.dataSize(desc));
    }

    /**
     * Modifies the method/constructor body.
     *
     * @param converter         specifies how to modify.
     */
    public void instrument(CodeConverter converter)
        throws CannotCompileException
    {
        declaringClass.checkModify();
        ConstPool cp = methodInfo.getConstPool();
        converter.doit(getDeclaringClass(), methodInfo, cp);
    }

    /**
     * Modifies the method/constructor body.
     *
     * @param editor            specifies how to modify.
     */
    public void instrument(ExprEditor editor)
        throws CannotCompileException
    {
        // if the class is not frozen,
        // does not turn the modified flag on.
        if (declaringClass.isFrozen())
            declaringClass.checkModify();

        if (editor.doit(declaringClass, methodInfo))
            declaringClass.checkModify();
    }

    /**
     * Inserts bytecode at the beginning of the body.
     *
     * <p>If this object represents a constructor,
     * the bytecode is inserted before
     * a constructor in the super class or this class is called.
     * Therefore, the inserted bytecode is subject to constraints described
     * in Section 4.8.2 of The Java Virtual Machine Specification (2nd ed).
     * For example, it cannot access instance fields or methods although
     * it may assign a value to an instance field directly declared in this
     * class.  Accessing static fields and methods is allowed.
     * Use <code>insertBeforeBody()</code> in <code>CtConstructor</code>.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     * @see CtConstructor#insertBeforeBody(String)
     */
    public void insertBefore(String src) throws CannotCompileException {
        declaringClass.checkModify();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        if (ca == null)
            throw new CannotCompileException("no method body");

        CodeIterator iterator = ca.iterator();
        Javac jv = new Javac(declaringClass);
        try {
            int nvars = jv.recordParams(getParameterTypes(),
                                        Modifier.isStatic(getModifiers()));
            jv.recordParamNames(ca, nvars);
            jv.recordLocalVariables(ca, 0);
            jv.compileStmnt(src);
            Bytecode b = jv.getBytecode();
            int stack = b.getMaxStack();
            int locals = b.getMaxLocals();

            if (stack > ca.getMaxStack())
                ca.setMaxStack(stack);

            if (locals > ca.getMaxLocals())
                ca.setMaxLocals(locals);

            int pos = iterator.insertEx(b.get());
            iterator.insert(b.getExceptionTable(), pos);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    /**
     * Inserts bytecode at the end of the body.
     * The bytecode is inserted just before every return insturction.
     * It is not executed when an exception is thrown.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     */
    public void insertAfter(String src)
        throws CannotCompileException
    {
        insertAfter(src, false);
    }

    /**
     * Inserts bytecode at the end of the body.
     * The bytecode is inserted just before every return insturction.
     *
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     * @param asFinally         true if the inserted bytecode is executed
     *                  not only when the control normally returns
     *                  but also when an exception is thrown.
     *                  If this parameter is true, the inserted code cannot
     *                  access local variables.
     */
    public void insertAfter(String src, boolean asFinally)
        throws CannotCompileException
    {
        declaringClass.checkModify();
        ConstPool pool = methodInfo.getConstPool();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        if (ca == null)
            throw new CannotCompileException("no method body");

        CodeIterator iterator = ca.iterator();
        int retAddr = ca.getMaxLocals();
        Bytecode b = new Bytecode(pool, 0, retAddr + 1);
        b.setStackDepth(ca.getMaxStack() + 1);
        Javac jv = new Javac(b, declaringClass);
        try {
            int nvars = jv.recordParams(getParameterTypes(),
                                        Modifier.isStatic(getModifiers()));
            jv.recordParamNames(ca, nvars);
            CtClass rtype = getReturnType0();
            int varNo = jv.recordReturnType(rtype, true);
            jv.recordLocalVariables(ca, 0);

            int handlerLen = insertAfterHandler(asFinally, b, rtype, varNo);

            byte[] save = makeSaveCode(pool, rtype, varNo);
            byte[] restore = makeRestoreCode(b, pool, rtype, varNo);

            b.addAstore(retAddr);
            jv.compileStmnt(src);
            b.addRet(retAddr);
            ca.setMaxStack(b.getMaxStack());
            ca.setMaxLocals(b.getMaxLocals());

            int gapPos = iterator.append(b.get());
            iterator.append(b.getExceptionTable(), gapPos);

            if (asFinally)
                ca.getExceptionTable().add(0, gapPos, gapPos, 0);

            int gapLen = iterator.getCodeLength() - gapPos - handlerLen;
            int subr = iterator.getCodeLength() - gapLen;

            while (iterator.hasNext()) {
                int pos = iterator.next();
                if (pos >= subr)
                    break;

                int c = iterator.byteAt(pos);
                if (c == Opcode.ARETURN || c == Opcode.IRETURN
                    || c == Opcode.FRETURN || c == Opcode.LRETURN
                    || c == Opcode.DRETURN || c == Opcode.RETURN) {
                    insertJSR(iterator, subr, pos, save, restore);
                    subr = iterator.getCodeLength() - gapLen;
                }
            }
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }

    private byte[] makeSaveCode(ConstPool cp, CtClass rtype, int varNo) {
        Bytecode b = new Bytecode(cp, 0, 0);
        if (rtype == CtClass.voidType) {
            b.addOpcode(Opcode.ACONST_NULL);
            b.addAstore(varNo);
            return b.get();
        }
        else {
            b.addStore(varNo, rtype);
            return b.get();
        }
    }

    private byte[] makeRestoreCode(Bytecode code, ConstPool cp,
                                   CtClass rtype, int varNo) {
        if (rtype == CtClass.voidType) {
            if (code.getMaxLocals() < 1)
                code.setMaxLocals(1);

            return new byte[0];
        }
        else {
            Bytecode b = new Bytecode(cp, 0, 0);
            b.addLoad(varNo, rtype);
            return b.get();
        }
    }

    private void insertJSR(CodeIterator iterator, int subr, int pos,
                           byte[] save, byte[] restore)
        throws BadBytecode
    {
        int gapSize = 5 + save.length + restore.length;
        boolean wide = subr - pos > Short.MAX_VALUE - gapSize - 4;
        gapSize = iterator.insertGap(pos, wide ? gapSize : gapSize - 2);

        iterator.write(save, pos);
        pos += save.length;
        if (wide) {
            iterator.writeByte(Opcode.JSR_W, pos);
            iterator.write32bit(subr - pos + gapSize, pos + 1);
            pos += 5;
        }
        else {
            iterator.writeByte(Opcode.JSR, pos);
            iterator.write16bit(subr - pos + gapSize, pos + 1);
            pos += 3;
        }

        iterator.write(restore, pos);
    }

    private int insertAfterHandler(boolean asFinally, Bytecode b,
                                   CtClass rtype, int returnVarNo)
    {
        if (!asFinally)
            return 0;

        int var = b.getMaxLocals();
        b.incMaxLocals(1);
        int pc = b.currentPc();
        b.addAstore(var);
        if (rtype.isPrimitive()) {
            char c = ((CtPrimitiveType)rtype).getDescriptor();
            if (c == 'D') {
                b.addDconst(0.0);
                b.addDstore(returnVarNo);
            }
            else if (c == 'F') {
                b.addFconst(0);
                b.addFstore(returnVarNo);
            }
            else if (c == 'J') {
                b.addLconst(0);
                b.addLstore(returnVarNo);
            }
            else if (c != 'V') { // int, boolean, char, short, ...
                b.addIconst(0);
                b.addIstore(returnVarNo);
            }
        }
        else {
            b.addOpcode(Opcode.ACONST_NULL);
            b.addAstore(returnVarNo);
        }

        b.addOpcode(Opcode.JSR);
        int pc2 = b.currentPc();
        b.addIndex(0);  // correct later
        b.addAload(var);
        b.addOpcode(Opcode.ATHROW);
        int pc3 = b.currentPc();
        b.write16bit(pc2, pc3 - pc2 + 1);
        return pc3 - pc;
    }

    /* -- OLD version --

    public void insertAfter(String src) throws CannotCompileException {
        declaringClass.checkModify();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator iterator = ca.iterator();
        Bytecode b = new Bytecode(methodInfo.getConstPool(),
                                  ca.getMaxStack(), ca.getMaxLocals());
        b.setStackDepth(ca.getMaxStack());
        Javac jv = new Javac(b, declaringClass);
        try {
            jv.recordParams(getParameterTypes(),
                            Modifier.isStatic(getModifiers()));
            CtClass rtype = getReturnType0();
            int varNo = jv.recordReturnType(rtype, true);
            boolean isVoid = rtype == CtClass.voidType;
            if (isVoid) {
                b.addOpcode(Opcode.ACONST_NULL);
                b.addAstore(varNo);
                jv.compileStmnt(src);
            }
            else {
                b.addStore(varNo, rtype);
                jv.compileStmnt(src);
                b.addLoad(varNo, rtype);
            }

            byte[] code = b.get();
            ca.setMaxStack(b.getMaxStack());
            ca.setMaxLocals(b.getMaxLocals());
            while (iterator.hasNext()) {
                int pos = iterator.next();
                int c = iterator.byteAt(pos);
                if (c == Opcode.ARETURN || c == Opcode.IRETURN
                    || c == Opcode.FRETURN || c == Opcode.LRETURN
                    || c == Opcode.DRETURN || c == Opcode.RETURN)
                    iterator.insert(pos, code);
            }
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }
    */

    /**
     * Adds a catch clause that handles an exception thrown in the
     * body.  The catch clause must end with a return or throw statement.
     *
     * @param src       the source code representing the catch clause.
     *                  It must be a single statement or block.
     * @param exceptionType     the type of the exception handled by the
     *                          catch clause.
     */
    public void addCatch(String src, CtClass exceptionType)
        throws CannotCompileException
    {
        addCatch(src, exceptionType, "$e");
    }

    /**
     * Adds a catch clause that handles an exception thrown in the
     * body.  The catch clause must end with a return or throw statement.
     *
     * @param src       the source code representing the catch clause.
     *                  It must be a single statement or block.
     * @param exceptionType     the type of the exception handled by the
     *                          catch clause.
     * @param exceptionName     the name of the variable containing the
     *                          caught exception, for example,
     *                          <code>$e</code>.
     */
    public void addCatch(String src, CtClass exceptionType,
                         String exceptionName)
        throws CannotCompileException
    {
        declaringClass.checkModify();
        ConstPool cp = methodInfo.getConstPool();
        CodeAttribute ca = methodInfo.getCodeAttribute();
        CodeIterator iterator = ca.iterator();
        Bytecode b = new Bytecode(cp, ca.getMaxStack(), ca.getMaxLocals());
        b.setStackDepth(1);
        Javac jv = new Javac(b, declaringClass);
        try {
            jv.recordParams(getParameterTypes(),
                            Modifier.isStatic(getModifiers()));
            int var = jv.recordVariable(exceptionType, exceptionName);
            b.addAstore(var);
            jv.compileStmnt(src);

            int stack = b.getMaxStack();
            int locals = b.getMaxLocals();

            if (stack > ca.getMaxStack())
                ca.setMaxStack(stack);

            if (locals > ca.getMaxLocals())
                ca.setMaxLocals(locals);

            int len = iterator.getCodeLength();
            int pos = iterator.append(b.get());
            ca.getExceptionTable().add(getStartPosOfBody(ca), len, len,
                                       cp.addClassInfo(exceptionType));
            iterator.append(b.getExceptionTable(), pos);
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
    }

    /* CtConstructor overrides this method.
     */
    int getStartPosOfBody(CodeAttribute ca) throws CannotCompileException {
        return 0;
    }

    /**
     * Inserts bytecode at the specified line in the body.
     * It is equivalent to:
     *
     * <br><code>insertAt(lineNum, true, src)</code>
     *
     * <br>See this method as well.
     *
     * @param lineNum   the line number.  The bytecode is inserted at the
     *                  beginning of the code at the line specified by this
     *                  line number.
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     * @return      the line number at which the bytecode has been inserted.
     *
     * @see CtBehavior#insertAt(int,boolean,String)
     */
    public int insertAt(int lineNum, String src)
        throws CannotCompileException
    {
        return insertAt(lineNum, true, src);
    }

    /**
     * Inserts bytecode at the specified line in the body.
     *
     * <p>If there is not
     * a statement at the specified line, the bytecode might be inserted
     * at the line including the first statement after that line specified.
     * For example, if there is only a closing brace at that line, the
     * bytecode would be inserted at another line below.
     * To know exactly where the bytecode will be inserted, call with
     * <code>modify</code> set to <code>false</code>. 
     *
     * @param lineNum   the line number.  The bytecode is inserted at the
     *                  beginning of the code at the line specified by this
     *                  line number.
     * @param modify    if false, this method does not insert the bytecode.
     *                  It instead only returns the line number at which
     *                  the bytecode would be inserted.
     * @param src       the source code representing the inserted bytecode.
     *                  It must be a single statement or block.
     *                  If modify is false, the value of src can be null.
     * @return      the line number at which the bytecode has been inserted.
     */
    public int insertAt(int lineNum, boolean modify, String src)
        throws CannotCompileException
    {
        CodeAttribute ca = methodInfo.getCodeAttribute();
        if (ca == null)
            throw new CannotCompileException("no method body");

        LineNumberAttribute ainfo
            = (LineNumberAttribute)ca.getAttribute(LineNumberAttribute.tag);
        if (ainfo == null)
            throw new CannotCompileException("no line number info");

        LineNumberAttribute.Pc pc = ainfo.toNearPc(lineNum);
        lineNum = pc.line;
        int index = pc.index;
        if (!modify)
            return lineNum;

        declaringClass.checkModify();
        CodeIterator iterator = ca.iterator();
        Javac jv = new Javac(declaringClass);
        try {
            jv.recordLocalVariables(ca, index);
            jv.recordParams(getParameterTypes(),
                            Modifier.isStatic(getModifiers()));
            jv.setMaxLocals(ca.getMaxLocals());
            jv.compileStmnt(src);
            Bytecode b = jv.getBytecode();
            int locals = b.getMaxLocals();
            int stack = b.getMaxStack();
            ca.setMaxLocals(locals);

            /* We assume that there is no values in the operand stack
             * at the position where the bytecode is inserted.
             */
            if (stack > ca.getMaxStack())
                ca.setMaxStack(stack);

            iterator.insert(index, b.get());
            iterator.insert(b.getExceptionTable(), index);
            return lineNum;
        }
        catch (NotFoundException e) {
            throw new CannotCompileException(e);
        }
        catch (CompileError e) {
            throw new CannotCompileException(e);
        }
        catch (BadBytecode e) {
            throw new CannotCompileException(e);
        }
    }
}
