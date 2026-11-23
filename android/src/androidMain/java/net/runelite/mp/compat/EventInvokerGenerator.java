package net.runelite.mp.compat;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Generates direct {@code <Target>$$EventInvoker} dispatch companions (implementing
 * {@code EventBus.ClassInvoker}) for classes carrying {@code @Subscribe} handlers, adding
 * package-private synthetic {@code $rlsub$N} bridges when handlers are {@code private}.
 * Uses ASM's core API so it runs both at build time ({@code RetargetBundledClient}) and on-device
 * ({@code PluginDexer}) without pulling asm-tree/commons where not already needed.
 */
public final class EventInvokerGenerator
{
	private static final String SUBSCRIBE_DESC = "Lnet/runelite/client/eventbus/Subscribe;";
	private static final String INVOKER_INTERNAL = "net/runelite/client/eventbus/EventBus$ClassInvoker";
	private static final String[] INVOKER_INTERFACES = {INVOKER_INTERNAL};
	private static final String BRIDGE_PREFIX = "$rlsub$";

	public static final class Result
	{
		public final byte[] classBytes;
		public final String invokerEntryName;
		public final byte[] invokerBytes;

		Result(byte[] classBytes, String invokerEntryName, byte[] invokerBytes)
		{
			this.classBytes = classBytes;
			this.invokerEntryName = invokerEntryName;
			this.invokerBytes = invokerBytes;
		}
	}

	private static final class SubMethod
	{
		final String name;
		final String descriptor;
		final String eventInternal;
		final String callName;

		SubMethod(String name, String descriptor, String eventInternal, String callName)
		{
			this.name = name;
			this.descriptor = descriptor;
			this.eventInternal = eventInternal;
			this.callName = callName;
		}
	}

	private EventInvokerGenerator()
	{
	}

	/**
	 * Inspects a compiled class and generates a direct invoker companion when any non-static,
	 * non-abstract {@code @Subscribe} single-object-parameter method is declared on it.
	 *
	 * @return generated invoker + optionally bridged target class bytes, or {@code null}
	 */
	public static Result transform(byte[] classBytes)
	{
		ClassReader reader = new ClassReader(classBytes);
		String classInternal = reader.getClassName();
		if ((reader.getAccess() & Opcodes.ACC_INTERFACE) != 0 || classInternal.endsWith("$$EventInvoker"))
		{
			return null;
		}

		List<SubMethod> subs = new ArrayList<>();
		boolean[] needsBridge = {false};
		reader.accept(new ClassVisitor(Opcodes.ASM9)
		{
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions)
			{
				if ((access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) != 0 || name.startsWith(BRIDGE_PREFIX))
				{
					return null;
				}
				String eventInternal = parseSingleObjectParamVoid(descriptor);
				if (eventInternal == null)
				{
					return null;
				}
				return new MethodVisitor(Opcodes.ASM9)
				{
					@Override
					public AnnotationVisitor visitAnnotation(String annDesc, boolean visible)
					{
						if (SUBSCRIBE_DESC.equals(annDesc))
						{
							boolean isPrivate = (access & Opcodes.ACC_PRIVATE) != 0;
							String callName = isPrivate ? (BRIDGE_PREFIX + subs.size()) : name;
							if (isPrivate)
							{
								needsBridge[0] = true;
							}
							subs.add(new SubMethod(name, descriptor, eventInternal, callName));
						}
						return null;
					}
				};
			}
		}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

		if (subs.isEmpty())
		{
			return null;
		}

		byte[] targetBytes = needsBridge[0] ? addPrivateBridges(reader, classInternal, subs) : classBytes;
		String invokerInternal = classInternal + "$$EventInvoker";
		byte[] invokerBytes = generateInvoker(classInternal, invokerInternal, subs);
		return new Result(targetBytes, invokerInternal + ".class", invokerBytes);
	}

	private static String parseSingleObjectParamVoid(String descriptor)
	{
		if (descriptor.length() < 6 || descriptor.charAt(0) != '(' || descriptor.charAt(1) != 'L' || !descriptor.endsWith(";)V"))
		{
			return null;
		}
		String inner = descriptor.substring(2, descriptor.length() - 3);
		return inner.indexOf(';') >= 0 ? null : inner;
	}

	private static byte[] addPrivateBridges(ClassReader reader, String classInternal, List<SubMethod> subs)
	{
		ClassWriter cw = new ClassWriter(reader, 0);
		reader.accept(new ClassVisitor(Opcodes.ASM9, cw)
		{
			@Override
			public void visitEnd()
			{
				for (SubMethod sub : subs)
				{
					if (!sub.callName.startsWith(BRIDGE_PREFIX))
					{
						continue;
					}
					MethodVisitor mv = cv.visitMethod(Opcodes.ACC_SYNTHETIC, sub.callName, sub.descriptor, null, null);
					mv.visitCode();
					mv.visitVarInsn(Opcodes.ALOAD, 0);
					mv.visitVarInsn(Opcodes.ALOAD, 1);
					mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classInternal, sub.name, sub.descriptor, false);
					mv.visitInsn(Opcodes.RETURN);
					mv.visitMaxs(2, 2);
					mv.visitEnd();
				}
				super.visitEnd();
			}
		}, 0);
		return cw.toByteArray();
	}

	private static byte[] generateInvoker(String classInternal, String invokerInternal, List<SubMethod> subs)
	{
		ClassWriter cw = new ClassWriter(0);
		cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
			invokerInternal, null, "java/lang/Object", INVOKER_INTERFACES);

		// Public no-arg constructor
		MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		init.visitCode();
		init.visitVarInsn(Opcodes.ALOAD, 0);
		init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		init.visitInsn(Opcodes.RETURN);
		init.visitMaxs(1, 1);
		init.visitEnd();

		// resolve(String methodName, Class<?> eventType) -> int
		MethodVisitor res = cw.visitMethod(Opcodes.ACC_PUBLIC, "resolve",
			"(Ljava/lang/String;Ljava/lang/Class;)I", null, null);
		res.visitCode();
		res.visitVarInsn(Opcodes.ALOAD, 2);
		res.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
		res.visitVarInsn(Opcodes.ASTORE, 3);

		int count = subs.size();
		Label[] labels = new Label[count + 1];
		for (int i = 0; i <= count; i++)
		{
			labels[i] = new Label();
		}
		Object[] resolveLocals = {invokerInternal, "java/lang/String", "java/lang/Class", "java/lang/String"};
		Object[] emptyStack = {};

		for (int i = 0; i < count; i++)
		{
			if (i > 0)
			{
				res.visitLabel(labels[i]);
				res.visitFrame(Opcodes.F_FULL, 4, resolveLocals, 0, emptyStack);
			}
			SubMethod sub = subs.get(i);
			res.visitVarInsn(Opcodes.ALOAD, 1);
			res.visitLdcInsn(sub.name);
			res.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
			res.visitJumpInsn(Opcodes.IFEQ, labels[i + 1]);
			res.visitVarInsn(Opcodes.ALOAD, 3);
			res.visitLdcInsn(sub.eventInternal.replace('/', '.'));
			res.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
			res.visitJumpInsn(Opcodes.IFEQ, labels[i + 1]);
			pushInt(res, i);
			res.visitInsn(Opcodes.IRETURN);
		}
		res.visitLabel(labels[count]);
		res.visitFrame(Opcodes.F_FULL, 4, resolveLocals, 0, emptyStack);
		res.visitInsn(Opcodes.ICONST_M1);
		res.visitInsn(Opcodes.IRETURN);
		res.visitMaxs(2, 4);
		res.visitEnd();

		// invoke(Object target, int slot, Object event) throws Exception
		MethodVisitor inv = cw.visitMethod(Opcodes.ACC_PUBLIC, "invoke",
			"(Ljava/lang/Object;ILjava/lang/Object;)V", null, new String[]{"java/lang/Exception"});
		inv.visitCode();
		Label defaultLabel = new Label();
		Label[] caseLabels = new Label[count];
		for (int i = 0; i < count; i++)
		{
			caseLabels[i] = new Label();
		}
		inv.visitVarInsn(Opcodes.ILOAD, 2);
		inv.visitTableSwitchInsn(0, count - 1, defaultLabel, caseLabels);
		for (int i = 0; i < count; i++)
		{
			SubMethod sub = subs.get(i);
			inv.visitLabel(caseLabels[i]);
			inv.visitFrame(Opcodes.F_SAME, 0, null, 0, emptyStack);
			inv.visitVarInsn(Opcodes.ALOAD, 1);
			inv.visitTypeInsn(Opcodes.CHECKCAST, classInternal);
			inv.visitVarInsn(Opcodes.ALOAD, 3);
			inv.visitTypeInsn(Opcodes.CHECKCAST, sub.eventInternal);
			inv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, classInternal, sub.callName, sub.descriptor, false);
			inv.visitInsn(Opcodes.RETURN);
		}
		inv.visitLabel(defaultLabel);
		inv.visitFrame(Opcodes.F_SAME, 0, null, 0, emptyStack);
		inv.visitInsn(Opcodes.RETURN);
		inv.visitMaxs(2, 4);
		inv.visitEnd();

		cw.visitEnd();
		return cw.toByteArray();
	}

	private static void pushInt(MethodVisitor mv, int v)
	{
		if (v >= -1 && v <= 5)
		{
			mv.visitInsn(Opcodes.ICONST_0 + v);
		}
		else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE)
		{
			mv.visitIntInsn(Opcodes.BIPUSH, v);
		}
		else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE)
		{
			mv.visitIntInsn(Opcodes.SIPUSH, v);
		}
		else
		{
			mv.visitLdcInsn(v);
		}
	}
}
