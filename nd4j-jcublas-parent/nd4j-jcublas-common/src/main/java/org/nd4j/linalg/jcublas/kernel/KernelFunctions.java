package org.nd4j.linalg.jcublas.kernel;


import jcuda.Pointer;
import jcuda.driver.*;

import static jcuda.driver.JCudaDriver.*;

import java.io.*;

import org.apache.commons.io.IOUtils;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Kernel functions.
 *
 * Derived from:
 * http://www.jcuda.org/samples/JCudaVectorAdd.java
 *
 * @author Adam Gibson
 */
public class KernelFunctions {


    private static Logger log = LoggerFactory.getLogger(KernelFunctions.class);

    private KernelFunctions() {}


    /**
     * Execute a cuda function
     * @param function the function to execute
     * @param blockSize the block size to execute on
     * @param gridSize the grid size to execute on
     * @param kernelParameters the kernel parameters
     */
    public static void exec(CUfunction function,int blockSize,int gridSize, Pointer kernelParameters) {
        cuLaunchKernel(function,
                blockSize,  1, 1,      // Grid dimension
                gridSize, 1, 1,      // Block dimension
                0, null,               // Shared memory size and stream
                kernelParameters, null // Kernel- and extra parameters
        );
        cuCtxSynchronize();


    }

    /**
     * Load the given file
     * @param fileName the file name
     * @param dataType the data type
     * @throws IOException
     */
    public static String load(String fileName,int dataType) throws IOException {
        // Enable exceptions and omit all subsequent error checks
        JCudaDriver.setExceptionsEnabled(true);

        // Create the PTX file by calling the NVCC
        String ptxFileName = preparePtxFile(fileName,dataType);

        // Initialize the driver and create a context for the first device.
        cuInit(0);
        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, 0, device);

        // Load the ptx file.
        CUmodule module = new CUmodule();
        cuModuleLoad(module, ptxFileName);

        return ptxFileName;


    }



    private static String dataFolder(int type) {
        return "/kernels/" +  (type == DataBuffer.FLOAT ? "float" : "double");
    }


    private static void extract(String file,int dataType) throws IOException {

        String path =  dataFolder(dataType);
        String tmpDir = System.getProperty("java.io.tmpdir");
        File dataDir = new File(tmpDir,path);
        if(!dataDir.exists())
            dataDir.mkdirs();
        ClassPathResource resource = new ClassPathResource(dataFolder(dataType) + "/" + file);
        if(!resource.exists())
            throw new IllegalStateException("Unable to find file " + resource);
        File out = new File(dataDir, file);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(out));
        IOUtils.copy(resource.getInputStream(),bos);
        bos.flush();
        bos.close();
        
        out.deleteOnExit();

    }


    /**
     * Load the function
     * @param deviceNumber the device number to load on
     * @param ptxFileName the ptx file name
     * @param functionName the function name to use as a handle
     */
    public static CUfunction loadFunction(int deviceNumber,String ptxFileName,String functionName) {
        // Initialize the driver and create a context for the first device.
        cuInit(deviceNumber);
        CUdevice device = new CUdevice();
        cuDeviceGet(device, deviceNumber);
        CUcontext context = new CUcontext();
        cuCtxCreate(context, deviceNumber, device);

        // Load the ptx file.
        CUmodule module = new CUmodule();
        cuModuleLoad(module, ptxFileName);

        // Obtain a function pointer to the "add" function.
        CUfunction function = new CUfunction();
        cuModuleGetFunction(function, module,functionName);
        return function;

    }


    /**
     * The extension of the given file name is replaced with "ptx".
     * If the file with the resulting name does not exist, it is
     * compiled from the given file using NVCC. The name of the
     * PTX file is returned.
     *
     *
     * Note that you may run in to an error akin to:
     * Unsupported GCC version
     *
     * At your own risk, comment the lines under:
     * /usr/local/cuda-$VERSION/include/host_config.h
     *
     * #if defined(__GNUC__)

    *    if __GNUC__ > 4 || (__GNUC__ == 4 && __GNUC_MINOR__ > 8)
     *   #error -- unsupported GNU version! gcc 4.9 and up are not supported!

     *     #endif /* __GNUC__> 4 || (__GNUC__ == 4 && __GNUC_MINOR__ > 8)

    *  #endif  __GNUC__

    *  This will allow you to bypass the compiler restrictions. Again, do so at your own risk.
     *
     *
     *
     * @param cuFileName The name of the .CU file
     * @return The name of the PTX file
     * @throws IOException If an I/O error occurs
     */
    private static String preparePtxFile(String cuFileName,int dataType) throws IOException {


        int endIndex = cuFileName.lastIndexOf('.');
        if (endIndex == -1) {
            endIndex = cuFileName.length() - 1;
        }

        String path =  dataFolder(dataType);
        String tmpDir = System.getProperty("java.io.tmpdir");
        File dataDir = new File(tmpDir,path);


        String ptxFileName = cuFileName.substring(0, endIndex + 1) + "ptx";
        File ptxFile = new File(dataDir,ptxFileName);
        if (ptxFile.exists()) {
            return ptxFileName;
        }


        else
             extract(cuFileName,dataType);


        File cuFile = new File(dataDir,cuFileName);
        if (!cuFile.exists())
            throw new IOException("Input file not found: " + cuFileName);



        String modelString = "-m"+System.getProperty("sun.arch.data.model");
        String command = "nvcc " + modelString + " -ptx "+ cuFile.getPath()+" -o "+ ptxFileName;

        log.info("Executing " + command);
        Process process = Runtime.getRuntime().exec(command);

        String errorMessage =
                new String(toByteArray(process.getErrorStream()));
        String outputMessage =
                new String(toByteArray(process.getInputStream()));
        int exitValue = 0;
        try
        {
            exitValue = process.waitFor();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IOException(
                    "Interrupted while waiting for nvcc output", e);
        }

        if (exitValue != 0) {
            log.info("nvcc process exitValue "+exitValue);
            log.info("errorMessage:\n"+errorMessage);
            log.info("outputMessage:\n"+outputMessage);
            throw new IOException(
                    "Could not create .ptx file: "+errorMessage);
        }

        log.info("Finished creating PTX file");
        return ptxFileName;
    }

    /**
     * Fully reads the given InputStream and returns it as a byte array
     *
     * @param inputStream The input stream to read
     * @return The byte array containing the data from the input stream
     * @throws IOException If an I/O error occurs
     */
    private static byte[] toByteArray(InputStream inputStream)
            throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buffer[] = new byte[8192];
        while (true)
        {
            int read = inputStream.read(buffer);
            if (read == -1)
            {
                break;
            }
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }


}