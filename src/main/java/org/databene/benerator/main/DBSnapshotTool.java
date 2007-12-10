/*
 * (c) Copyright 2007 by Volker Bergmann. All rights reserved.
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

package org.databene.benerator.main;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.databene.commons.NumberUtil;
import org.databene.commons.RoundedNumberFormat;
import org.databene.model.data.Entity;
import org.databene.model.data.EntityDescriptor;
import org.databene.platform.db.adapter.DBSystem;
import org.databene.platform.dbunit.DbUnitEntityExporter;

/**
 * Creates a snapshot of a database schema and exports it in DbUnit XML file format.
 * @author Volker Bergmann
 * @since 0.3.04
 */
public class DBSnapshotTool {
    
    private static final Log logger = LogFactory.getLog(DBSnapshotTool.class);
    
    public static void main(String[] args) {

        String filename = (args.length > 0 ? args[0] : "snapshot.dbunit.xml");
        String fileEncoding = System.getProperty("file.encoding");
        
        String dbUrl = System.getProperty("db.url");
        String dbDriver = System.getProperty("db.driver");
        String dbUser = System.getProperty("db.user");
        String dbPassword = System.getProperty("db.password");
        String dbSchema = System.getProperty("db.schema");
        
        logger.info("Exporting data of database " + dbUrl + " with driver " + dbDriver + " as user " + dbUser 
                + (dbSchema != null ? " using schema " + dbSchema : "") 
                + " to file " + filename + " using " + fileEncoding);

        long startTime = System.currentTimeMillis();
        DbUnitEntityExporter exporter = new DbUnitEntityExporter(filename, fileEncoding);

        DBSystem db = null;
        int count = 0;
        try {
            db = new DBSystem("db", dbUrl, dbDriver, dbUser, dbPassword);
            if (dbSchema != null)
                db.setSchema(dbSchema);
            List<EntityDescriptor> descriptors = Arrays.asList(db.getTypeDescriptors());
            logger.info("Starting export");
            for (EntityDescriptor descriptor : descriptors) {
                logger.info("Exporting table " + descriptor.getName());
                for (Entity entity : db.getEntities(descriptor.getName())) {
                    exporter.process(entity);
                    count++;
                }
            }
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Created " + NumberUtil.format(count, 0) + " entities in " + RoundedNumberFormat.format(duration, 0) + " ms (" + RoundedNumberFormat.format(count * 3600000L / duration, 0) + " p.h.)");
        } finally {
            exporter.close();
            if (db != null)
                db.close();
        }
    }
}
