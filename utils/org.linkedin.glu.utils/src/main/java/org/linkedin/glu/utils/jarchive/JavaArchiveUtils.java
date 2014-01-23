package org.linkedin.glu.utils.jarchive;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
/**
 * The JavaArchiveUtils exposes methods to archive Zip or tar or gz file
 * Use:
 *  - extractZipFile(File zipfile, File ir) mthod for Zip
 *  - extractTarFile(File tarfile, File outdir) method for .tar.gz
 *
 */
public class JavaArchiveUtils {
	 
	/**
	 * Extract tarfile to outdir with complete directory structure
	 * @param tarfile Input .tar.gz file
	 * @param destFolder Output directory - if this is null the extract in the same folder where .tar.gz file is
	*/
	public static void extractTarFile(File tarfile, String destFolder ){
		BufferedOutputStream destStream = null;
        JavaArchiveEntry jArchiveEntry = null;
        JavaArchiveInputStream jTarInputStream = null;
        GZIPInputStream gzipStream = null;
        FileInputStream fileinput = null;
        BufferedInputStream buffStream = null;
        
        try{
        	if(!tarfile.exists()){
     			throw new RuntimeException("gz file does not exist.");
     		}
     		if(destFolder == null || destFolder.isEmpty()){
                throw new RuntimeException("Destination  location is missing.");
     		}

     		fileinput = new FileInputStream(tarfile);
     		gzipStream = new GZIPInputStream(fileinput);
     		buffStream = new BufferedInputStream(gzipStream);
     		jTarInputStream = new JavaArchiveInputStream(buffStream);
            
            while ((jArchiveEntry = jTarInputStream.getNextEntry()) != null) {
              System.out.println("Extracting: " + jArchiveEntry.getName());
              int count = 0;
              byte[] data = new byte[JavaArchiveConstants.BUFFER_SIZE];            
              if (jArchiveEntry.isDirectory()) {
                  new File(destFolder + "/" + jArchiveEntry.getName()).mkdirs();
                  continue;
              } else {
                  int di = jArchiveEntry.getName().lastIndexOf('/');
                  if (di != -1) {
                      new File(destFolder + "/"+ jArchiveEntry.getName().substring(0, di)).mkdirs();
                  }
              }
              
              FileOutputStream fos = null;
              try{
                    fos = new FileOutputStream(destFolder + "/"+ jArchiveEntry.getName());
                    destStream = new BufferedOutputStream(fos);

                    while ((count = jTarInputStream.read(data)) != -1) {
                        destStream.write(data, 0, count);
                    }
              }catch(Exception exp){
                  throw exp;
              }finally{
                if(destStream!=null){
                	destStream.flush();
                	destStream.close();
                }
                if(fos!=null){fos.close();}
               }
            }
        }catch(Exception exp){
        	exp.printStackTrace();
        	throw new RuntimeException(exp);
        }finally{
            try{
            	if(fileinput!=null){fileinput.close();}
            	if(gzipStream!=null){gzipStream.close();}
            	if(buffStream!=null){buffStream.close();}
	        	if(destStream!=null){destStream.close();}
	            if(jTarInputStream != null){jTarInputStream.close();}
            }catch(Exception exp){
            	throw new RuntimeException(exp);
            }
        }
	}
	
	
	/**
	 * Extract zipfile to outdir with complete directory structure
	 * @param zipfile Input .zip file
	 * @param outdir Output directory - if this is null the extract in the same folder where zip file is
	 */
	public static void extractZipFile(File zipfile, File outdir) {
		ZipInputStream zin = null;
		try {
			if(!zipfile.exists()){
				throw new RuntimeException("Zip file does not exist.");
			}
			if(outdir == null){
				outdir = zipfile.getParentFile();	
			}else if (!outdir.exists()){
				outdir.mkdirs();
			}
			zin = new ZipInputStream(new FileInputStream(zipfile));
			ZipEntry entry;
			String name, dir;
			while ((entry = zin.getNextEntry()) != null) {
				name = entry.getName();
				if (entry.isDirectory()) {
					mkdirs(outdir, name);
					continue;
				}
				dir = dirpart(name);
				if (dir != null)
					mkdirs(outdir, dir);
				extractZipFile(zin, outdir, name);
			}
			zin.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}finally{
			if(zin != null)
				try {
					zin.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
		}
	}
		
		
	/**
	 * 
	 * @param in
	 * @param outdir
	 * @param name
	 * @throws java.io.IOException
	 */
	private static void extractZipFile(ZipInputStream in, File outdir,
			String name) throws IOException {
		BufferedOutputStream out = null;
		try {
			byte[] buffer = new byte[JavaArchiveConstants.BUFFER_SIZE];
			out = new BufferedOutputStream(new FileOutputStream(new File(
					outdir, name)));
			int count = -1;
			while ((count = in.read(buffer)) != -1) {
				out.write(buffer, 0, count);
			}
		} catch (Exception exp) {
			throw new RuntimeException(exp);
		} finally {
			if (out != null)
				out.close();
		}
	}
	
	/**
	 * 
	 * @param outdir
	 * @param path
	 */
	private static void mkdirs(File outdir, String path) {
		File d = new File(outdir, path);
		if (!d.exists())
			d.mkdirs();
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	private static String dirpart(String name) {
		int s = name.lastIndexOf(File.separatorChar);
		return s == -1 ? null : name.substring(0, s);
	}
}
