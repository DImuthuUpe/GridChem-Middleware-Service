package org.gridchem.service.file;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.globus.common.ChainedIOException;
import org.globus.ftp.FTPClient;
import org.globus.ftp.OutputStreamDataSource;
import org.globus.ftp.Session;
import org.globus.ftp.exception.FTPException;
import org.globus.ftp.vanilla.TransferState;
import org.globus.io.streams.GlobusOutputStream;

public class FTPOutputStream extends GlobusOutputStream {
	
	protected static final Logger logger = LogManager.getLogger(FTPInputStream.class);
	
	protected OutputStream output;

	protected FTPClient ftp;

	protected TransferState state;

	protected String outFile = "";

	protected FTPOutputStream() {
	}

	public FTPOutputStream(FTPClient client, String file, boolean passive,
			boolean append) throws IOException, FTPException {
		this(client, file, passive, Session.TYPE_IMAGE, append);
	}

	public FTPOutputStream(FTPClient client, String file, boolean passive,
			int type, boolean append) throws IOException, FTPException {
		this.ftp = client;
		this.outFile = file;
		put(passive, type, file, append);
	}

	public void abort() {
		if (this.output != null) {
			try {
				this.output.close();
			} catch (Exception e) {
			}
		}
		try {
			//this.ftp.close();
			this.ftp.abort();
		} catch (IOException e) {
		} catch (FTPException e) {
		}
	}

	public void close() throws IOException {

		if (this.output != null) {
			try {
				this.output.close();
			} catch (Exception e) {
			}
		}

		try {
			if (this.state != null) {
				this.state.waitForEnd();
			}
		} catch (FTPException e) {
			throw new ChainedIOException("Output stream closed.", e);
		}
	}

	protected void put(boolean passive, int type, String remoteFile,
			boolean append) throws IOException, FTPException {
		OutputStreamDataSource source = null;
		try {
			this.ftp.setType(type);

			if (passive) {
				this.ftp.setPassive();
				this.ftp.setLocalActive();
			} else {
				this.ftp.setLocalPassive();
				this.ftp.setActive();
			}

			source = new OutputStreamDataSource(2048);

			this.state = this.ftp.asynchPut(remoteFile, source, null, append);

			this.state.waitForStart();

			this.output = source.getOutputStream();
		} catch (FTPException e) {
			if (source != null) {
				source.close();
			}
			close();
			throw e;
		}
	}

	public void write(byte[] msg) throws IOException {
		this.output.write(msg);
	}

	public void write(byte[] msg, int from, int length) throws IOException {
		this.output.write(msg, from, length);
	}

	public void write(int b) throws IOException {
		this.output.write(b);
	}

	public void flush() throws IOException {
		this.output.flush();
	}

}