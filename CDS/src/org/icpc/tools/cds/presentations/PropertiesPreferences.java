package org.icpc.tools.cds.presentations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

import org.icpc.tools.contest.Trace;

public class PropertiesPreferences extends AbstractPreferences {
	protected Properties props;
	protected File file;

	protected PropertiesPreferences(String name) {
		super(null, "");
		file = new File(System.getProperty("java.io.tmpdir"), name + ".prefs");
	}

	protected PropertiesPreferences(File file, Properties props, AbstractPreferences parent, String name) {
		super(parent, name);
		this.file = file;
		this.props = props;
	}

	@Override
	protected AbstractPreferences childSpi(String name) {
		checkLoaded();
		return new PropertiesPreferences(file, props, this, name + ".");
	}

	@Override
	protected String[] childrenNamesSpi() throws BackingStoreException {
		throw new BackingStoreException("Not implemented");
	}

	@Override
	protected void flushSpi() throws BackingStoreException {
		if (props == null)
			return;

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			props.store(fos, null);
		} catch (IOException ioe) {
			throw new BackingStoreException(ioe);
		} finally {
			try {
				if (fos != null)
					fos.close();
			} catch (Exception e) {
				// ignore
			}
		}
	}

	@Override
	protected String getSpi(String key) {
		checkLoaded();
		return props.getProperty(name() + key);
	}

	@Override
	protected String[] keysSpi() throws BackingStoreException {
		checkLoaded();
		List<String> list = new ArrayList<>();
		for (Object o : props.keySet()) {
			String key = o.toString();
			if (key.startsWith(name()))
				list.add(key.substring(name().length()));
		}
		return list.toArray(new String[0]);
	}

	@Override
	protected void putSpi(String key, String value) {
		checkLoaded();
		props.setProperty(name() + key, value);
	}

	@Override
	protected void removeNodeSpi() throws BackingStoreException {
		throw new BackingStoreException("Not implemented");
	}

	@Override
	protected void removeSpi(String key) {
		checkLoaded();
		props.remove(name() + key);
	}

	@Override
	protected void syncSpi() throws BackingStoreException {
		flushSpi();
	}

	protected synchronized void checkLoaded() {
		if (props != null)
			return;

		props = new Properties();

		if (file.exists()) {
			FileInputStream fin = null;
			try {
				fin = new FileInputStream(file);
				props.load(fin);
			} catch (IOException e) {
				Trace.trace(Trace.ERROR, "Error loading properties", e);
			} finally {
				try {
					if (fin != null)
						fin.close();
				} catch (Exception e) {
					// ignore
				}
			}
		}
	}
}