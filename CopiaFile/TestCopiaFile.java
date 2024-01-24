import java.util.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.security.spec.InvalidParameterSpecException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;


public class TestCopiaFile {
    
    
    public static void main(String [] args) {
        try {
            if(args.length!=1) {
                throw new IllegalArgumentException("fornire il nome di essattamente un file");
            }
            File filein = new File(args[0]);

            CopiaFile channeldiretto = new CopiaFileChannelDiretto(filein);
            channeldiretto.copia();

            CopiaFile channelindiretto = new CopiaFileChannelIndiretto(filein);
            channelindiretto.copia();

            CopiaFile channeltransfer = new CopiaFileChannelTransfer(filein);
            channeltransfer.copia();

            CopiaFile bufferedstream = new CopiaFileBufferedStream(filein);
            bufferedstream.copia();

            CopiaFile bytearray = new CopiaFileByteArray(filein);
            bytearray.copia();

        }
        catch(Throwable e) {
            e.printStackTrace();
        }
        
    }
    
    

}


class DifferenzaTempo {
    private long tinizio, tfine;
    private String mezzo;
    public DifferenzaTempo(String mezzo) {
        this.tinizio = 0;
        this.tfine = 0;
        this.mezzo = mezzo;
    }

    public void setinizio() {
        this.tinizio = System.nanoTime();
    }

    public void setfine() {
        this.tfine = System.nanoTime();
    }

    public double getdurata() {
        return (tfine / 1000000.0)-(tinizio / 1000000.0);
    }

    public String toString() {
        return mezzo + " ha impiegato " + getdurata() + " msec\n";
    }
}


interface CopiaFile {
    void copia();
    default File creafileoutput(File file, String mezzo) {
        String nomefile = file.getName();
        String [] array = nomefile.split("\\.",2);
        if(array.length!=1) {
            nomefile = array[0] + mezzo + "." + array[1];
        }
        else {
            nomefile+=mezzo;
        }
        return new File(nomefile);
    };
}

class CopiaFileChannelDiretto implements CopiaFile {
    private File filein;
    public CopiaFileChannelDiretto(File filein) {
        this.filein = filein;
    }

    @Override
    public void copia() {
        DifferenzaTempo diff = new DifferenzaTempo("channel diretto");
        diff.setinizio();
        try (ReadableByteChannel source = Channels.newChannel(
                new FileInputStream(filein.getName()));
            WritableByteChannel dest = Channels.newChannel (
                new FileOutputStream(creafileoutput(filein, "channeldiretto")))) {

            ByteBuffer buffer = ByteBuffer.allocateDirect (16 * 1024);
            while (source.read(buffer)!= -1) {
                buffer.flip();

                while(buffer.hasRemaining()) {
                    dest.write(buffer);
                }
                buffer.clear();
            }
        }
        catch(Throwable e) {
            e.printStackTrace();
        }
        finally {
            diff.setfine();
            System.out.println(diff.toString());
        }
    }
}


class CopiaFileChannelIndiretto implements CopiaFile {
    private File filein;
    public CopiaFileChannelIndiretto(File filein) {
        this.filein = filein;
    }

    @Override
    public void copia() {
        DifferenzaTempo diff = new DifferenzaTempo("channel indiretto");
        diff.setinizio();
        try (ReadableByteChannel source = Channels.newChannel(
                new FileInputStream(filein.getName()));
            WritableByteChannel dest = Channels.newChannel (
                new FileOutputStream(creafileoutput(filein, "channelindiretto")))) {

            ByteBuffer buffer = ByteBuffer.allocate(16 * 1024);
            while (source.read(buffer)!= -1) {
                buffer.flip();

                while(buffer.hasRemaining()) {
                    dest.write(buffer);
                }
                buffer.clear();
            }
        }
        catch(Throwable e) {
            e.printStackTrace();
        }
        finally {
            diff.setfine();
            System.out.println(diff.toString());
        }
    }
}


class CopiaFileChannelTransfer implements CopiaFile {
    private File filein;
    public CopiaFileChannelTransfer(File filein) {
        this.filein = filein;
    }

    @Override
    public void copia() {
        DifferenzaTempo diff = new DifferenzaTempo("channel con transfer");
        diff.setinizio();
        try(FileInputStream src = new FileInputStream(filein); 
            FileOutputStream dest = new FileOutputStream(creafileoutput(filein, "transfer"));) {

            FileChannel fromChannel = src.getChannel();
            FileChannel toChannel = dest.getChannel();
            long position = 0;
            long count = fromChannel.size();
            long trasferiti = 0;
            long letti;
            while(trasferiti!=count) {
                letti = toChannel.transferFrom(fromChannel, position, count);
                trasferiti+=letti;
            }
        }
        catch(Throwable e) {
            e.printStackTrace();
        }
        finally {
            diff.setfine();
            System.out.println(diff.toString());
        }
    }
}


class CopiaFileBufferedStream implements CopiaFile {
    private File filein;
    public CopiaFileBufferedStream(File filein) {
        this.filein = filein;
    }

    @Override
    public void copia() {
        DifferenzaTempo diff = new DifferenzaTempo("Buffered Stream");
        diff.setinizio();
        try (BufferedInputStream bufferedInput = new BufferedInputStream(
            new FileInputStream(filein)); BufferedOutputStream bufferedOutput = 
            new BufferedOutputStream(new FileOutputStream(
            creafileoutput(filein, "bufferedstream")));) {
            
            byte[] buffer = new byte[1024];
            int byteletti;

            while ((byteletti = bufferedInput.read(buffer)) != -1) {
                bufferedOutput.write(buffer, 0, byteletti);
            }
        }
        catch(Throwable e) {
            e.printStackTrace();
        }
        finally {
            diff.setfine();
            System.out.println(diff.toString());
        }
    }
}

class CopiaFileByteArray implements CopiaFile {
    private File filein;
    public CopiaFileByteArray(File filein) {
        this.filein = filein;
    }

    @Override
    public void copia() {
        DifferenzaTempo diff = new DifferenzaTempo("Byte Array");
        diff.setinizio();
        try (FileInputStream input =
            new FileInputStream(filein); FileOutputStream output = 
            new FileOutputStream(creafileoutput(filein, "bytearray"));) {
    
            byte[] buffer = new byte[1024];
            int byteletti;

            while ((byteletti = input.read(buffer)) != -1) {
                output.write(buffer, 0, byteletti);
            }
        }
        catch(Throwable e) {
            e.printStackTrace();
        }
        finally {
            diff.setfine();
            System.out.println(diff.toString());
        }
    }
}
