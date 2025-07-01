package ch.psi.pshell.xscan;

import ch.psi.pshell.framework.Context;
import ch.psi.pshell.utils.EventBusListener;
import ch.psi.pshell.utils.Message;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Serialize data received by a DataQueue
 */
public class SerializerTXT implements EventBusListener{

	private static final Logger logger = Logger.getLogger(SerializerTXT.class.getName());

	private File file;
	private boolean appendSuffix = true;
        private boolean flush = false;

	private boolean first = true;
	private File outfile;

	private int icount;
	private String basename;
	private String extension;
	private boolean newfile;
	private boolean dataInBetween;
	private BufferedWriter writer;
	private StringBuffer b;
	private StringBuffer b1;
	private boolean showDimensionHeader = true;

	public SerializerTXT(File file) {
		this.file = file;
	}

	/**
	 * @param metadata
	 * @param file
	 * @param appendSuffix
	 *            Flag whether to append a _0000 suffix after the original file
	 *            name
	 */
	public SerializerTXT(File file, boolean appendSuffix, boolean flush) {
		this.file = file;
		this.appendSuffix = appendSuffix;
                this.flush = flush;
	}

	//@Subscribe
        @Override
	public void onMessage(Message message) {
		try {
			if (first) {
				first = false;
				// Write header

				icount = 0;
				newfile = true;
				dataInBetween = false;
				writer = null;

				// Get basename of the file
				basename = this.file.getAbsolutePath(); // Determine file name
				extension = basename.replaceAll("^.*\\.", ""); // Determine
																// extension
				basename = basename.replaceAll("\\." + extension + "$", "");
			}

			if (message instanceof DataMessage dataMessage) {
				dataInBetween = true;
				if (newfile) {

					b = new StringBuffer();
					b1 = new StringBuffer();
					b.append("#");
					b1.append("#");
					for (Metadata c : dataMessage.getMetadata()) {

						b.append(c.getId());
						b.append("\t");

						b1.append(c.getDimension());
						b1.append("\t");
					}
					b.setCharAt(b.length() - 1, '\n');
					b1.setCharAt(b1.length() - 1, '\n');

					// Open new file and write header
					// Construct file name
					if (appendSuffix) {
						outfile = new File(String.format("%s_%04d.%s", basename, icount, extension));
					}
					else {
						outfile = new File(String.format("%s.%s", basename, extension));
					}

					// Open file
					logger.log(Level.FINE, "Open new data file: {0}", outfile.getAbsolutePath());
					writer = new BufferedWriter(new FileWriter(outfile));

					// Write header
					writer.write(b.toString());
					if (showDimensionHeader) {
						writer.write(b1.toString());
					}

					newfile = false;
                                        Context.addDetachedFileToSession(outfile);
				}

				// Write message to file - each message will result in one line
				DataMessage m = dataMessage;
				StringBuffer buffer = new StringBuffer();
				for (Object o : m.getData()) {
					if (o.getClass().isArray()) {
						// If the array object is of type double[] display its
						// content
						if (o instanceof double[] oa) {
							for (double o1 : oa) {
								buffer.append(o1);
								buffer.append(" "); // Use space instead of tab
							}
                                                        // Replace last space with tab
							buffer.replace(buffer.length() - 1, buffer.length() - 1, "\t"); 
						}
						else if (o instanceof Object[] oa) {
							for (Object o1 : oa) {
								buffer.append(o1);
								buffer.append(" "); // Use space instead of tab
							}
                                                        // Replace last space with tab
							buffer.replace(buffer.length() - 1, buffer.length() - 1, "\t");	
                                                }
						else {
							buffer.append("-"); // Not supported
						}
					}
					else {
						buffer.append(o);
						buffer.append("\t");
					}
				}

				if (buffer.length() > 0) {
					buffer.deleteCharAt(buffer.length() - 1); // Remove last
																// character
																// (i.e. \t)
					buffer.append("\n"); // Append newline
				}
				writer.write(buffer.toString());
                                if (flush){                                             
                                    writer.flush();
                                }
			}
			else if (message instanceof StreamDelimiterMessage m) {
				logger.log(Level.FINER, "Delimiter - number: {0} iflag: {1}", new Object[]{m.getNumber(), m.isIflag()});
				if (m.isIflag() && appendSuffix) {
					// Only increase iflag counter if there was data in between
					// subsequent StreamDelimiterMessages.
					if (dataInBetween) {
						icount++;
					}
					dataInBetween = false;

					// Set flag to open new file
					newfile = true;

					// Close file
					writer.close();
				}
			}
			else if (message instanceof EndOfStreamMessage) {
				if (writer != null) {
					// Close file
					writer.close(); // If the stream was closed previously this
									// has no effect
				}
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Data serializer had a problem writing to the specified file", e);
			throw new RuntimeException("Data serializer had a problem writing to the specified file", e);
		}
	}

	public boolean isShowDimensionHeader() {
		return showDimensionHeader;
	}

	public void setShowDimensionHeader(boolean showDimensionHeader) {
		this.showDimensionHeader = showDimensionHeader;
	}
}
