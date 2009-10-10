/*
 * (c) Copyright 2009 by Volker Bergmann. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted under the terms of the
 * GNU General Public License.
 *
 * For redistributing this software or a derivative work under a license other
 * than the GPL-compatible Free Software License as defined by the Free
 * Software Foundation or approved by OSI, you must first obtain a commercial
 * license to this software product from Volker Bergmann.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * WITHOUT A WARRANTY OF ANY KIND. ALL EXPRESS OR IMPLIED CONDITIONS,
 * REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE
 * HEREBY EXCLUDED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.databene.platform.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.databene.commons.ArrayFormat;
import org.databene.commons.Assert;
import org.databene.commons.Context;
import org.databene.commons.IOUtil;
import org.databene.task.AbstractTask;

/**
 * Joins several source files into a destination file. 
 * If the property 'append' is 'true' and the destination file already exists, 
 * it will append the source files' contents to the existing file.<br/>
 * <br/>
 * Created at 16.09.2009 15:50:25
 * @since 0.6.0
 * @author Volker Bergmann
 */

public class FileJoiner extends AbstractTask {
	
	private static final int BUFFER_SIZE = 65536;
	
	private String[] sources = new String[0];
	private String destination = null;
	private boolean append = false;
	private boolean deleteSources = false;
	
	// properties ------------------------------------------------------------------------------------------------------

    public String[] getSources() {
    	return sources;
    }

    public void setSources(String[] sources) {
    	this.sources = sources;
    }

    public String getDestination() {
    	return destination;
    }

    public void setDestination(String destination) {
    	this.destination = destination;
    }

    public boolean isAppend() {
    	return append;
    }

    public void setAppend(boolean append) {
    	this.append = append;
    }
    
    public boolean isDeleteSources() {
    	return deleteSources;
    }

    public void setDeleteSources(boolean deleteSources) {
    	this.deleteSources = deleteSources;
    }

	// Task interface implementation -----------------------------------------------------------------------------------

	public void run(Context context) {
		Assert.notNull(destination, "property 'destination'");
		byte[] buffer = new byte[BUFFER_SIZE];
		OutputStream out = null;
		try {
			File destFile = new File(destination); // TODO support relative uris
			out = new FileOutputStream(destFile, append);
			for (String source : sources) 
				appendFile(out, source, buffer);
			if (deleteSources)
				for (String source : sources) {
					File file = new File(source);
					if (!file.delete())
						getErrorHandler(context).handleError("File could not be deleted: " + file + ". " +
								"Probably it is locked");
				}
		} catch (Exception e) {
			getErrorHandler(context).handleError("Error joining the files " + ArrayFormat.format(sources), e);
        } finally {
			IOUtil.close(out);
		}
    }
    
    // private helpers -------------------------------------------------------------------------------------------------

    private void appendFile(OutputStream out, String source, byte[] buffer) throws FileNotFoundException, IOException {
	    FileInputStream in = null;
	    try {
	    	in = new FileInputStream(source); // TODO support relative uris
	    	int partLength = 0;
	    	while ((partLength = in.read(buffer)) > 0)
	    		out.write(buffer, 0, partLength);
	    } finally {
	    	IOUtil.close(in);
	    }
    }

}
