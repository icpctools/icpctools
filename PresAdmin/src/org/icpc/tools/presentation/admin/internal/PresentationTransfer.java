package org.icpc.tools.presentation.admin.internal;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.icpc.tools.presentation.core.internal.PresentationInfo;

public class PresentationTransfer extends ByteArrayTransfer {
	// create a UUID for the type name to make sure that different Eclipse applications
	// use different "types" of <code>PresentationTransfer</code>
	private static final String TYPE_NAME = "presentation-transfer-format" + System.currentTimeMillis();
	private static final int TYPEID = registerType(TYPE_NAME);

	private static final PresentationTransfer INSTANCE = new PresentationTransfer();

	private PresentationInfo selection;
	private Image image;

	/**
	 * Only the singleton instance of this class may be used.
	 */
	protected PresentationTransfer() {
		// do nothing
	}

	/**
	 * Returns the singleton.
	 *
	 * @return the singleton
	 */
	public static PresentationTransfer getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns the local transfer data.
	 *
	 * @return the local transfer data
	 */
	public PresentationInfo getSelection() {
		return selection;
	}

	/**
	 * Tests whether native drop data matches this transfer type.
	 *
	 * @param result result of converting the native drop data to Java
	 * @return true if the native drop data does not match this transfer type. false otherwise.
	 */
	private static boolean isInvalidNativeType(Object result) {
		return !(result instanceof byte[]) || !TYPE_NAME.equals(new String((byte[]) result));
	}

	/**
	 * Returns the type id used to identify this transfer.
	 *
	 * @return the type id used to identify this transfer.
	 */
	@Override
	protected int[] getTypeIds() {
		return new int[] { TYPEID };
	}

	/**
	 * Returns the type name used to identify this transfer.
	 *
	 * @return the type name used to identify this transfer.
	 */
	@Override
	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

	/**
	 * Overrides org.eclipse.swt.dnd.ByteArrayTransfer#javaToNative(Object, TransferData). Only
	 * encode the transfer type name since the selection is read and written in the same process.
	 *
	 * @see org.eclipse.swt.dnd.ByteArrayTransfer#javaToNative(java.lang.Object,
	 *      org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public void javaToNative(Object object, TransferData transferData) {
		byte[] check = TYPE_NAME.getBytes();
		super.javaToNative(check, transferData);
	}

	/**
	 * Overrides org.eclipse.swt.dnd.ByteArrayTransfer#nativeToJava(TransferData). Test if the
	 * native drop data matches this transfer type.
	 *
	 * @see org.eclipse.swt.dnd.ByteArrayTransfer#nativeToJava(TransferData)
	 */
	@Override
	public Object nativeToJava(TransferData transferData) {
		Object result = super.nativeToJava(transferData);
		if (isInvalidNativeType(result)) {
			// should never happen
		}
		return selection;
	}

	/**
	 * Sets the transfer data for local use.
	 *
	 * @param s the transfer data
	 */
	public void setSelection(PresentationInfo s, Image image) {
		selection = s;
		if (this.image != null)
			this.image.dispose();
		this.image = image;
	}
}