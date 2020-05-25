package com.squareup.okhttp.internal.bytes;
import java.io.EOFException;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Inflater;
public final class GzipSource implements Source {
  public static byte FHCRC=1;
  public static byte FEXTRA=2;
  public static byte FNAME=3;
  public static byte FCOMMENT=4;
  public static byte SECTION_HEADER=0;
  public static byte SECTION_BODY=1;
  public static byte SECTION_TRAILER=2;
  public static byte SECTION_DONE=3;
  /** 
 * The current section. Always progresses forward. 
 */
  public int section=SECTION_HEADER;
  /** 
 * This buffer is carefully shared between this source and the InflaterSource it wraps. In particular, this source may read more bytes than necessary for the GZIP header; the InflaterSource will pick those up when it starts to read the compressed body. And the InflaterSource may read more bytes than necessary for the compressed body, and this source will pick those up for the GZIP trailer.
 */
  public OkBuffer buffer=new OkBuffer();
  /** 
 * Our source should yield a GZIP header (which we consume directly), followed by deflated bytes (which we consume via an InflaterSource), followed by a GZIP trailer (which we also consume directly).
 */
  public Source source;
  /** 
 * The inflater used to decompress the deflated body. 
 */
  public Inflater inflater;
  /** 
 * The inflater source takes care of moving data between compressed source and decompressed sink buffers.
 */
  public InflaterSource inflaterSource;
  /** 
 * Checksum used to check both the GZIP header and decompressed body. 
 */
  public CRC32 crc=new CRC32();
  public GzipSource(  Source source) throws IOException {
    this.inflater=new Inflater(true);
    this.source=source;
    this.inflaterSource=new InflaterSource(source,inflater,buffer);
  }
  @Override public long read(  OkBuffer sink,  long byteCount,  Deadline deadline) throws IOException {
    if (byteCount < 0)     throw new IllegalArgumentException("byteCount < 0: " + byteCount);
    if (byteCount == 0)     return 0;
    if (section == SECTION_HEADER) {
      consumeHeader(deadline);
      section=SECTION_BODY;
    }
    if (section == SECTION_BODY) {
      long offset=sink.byteCount;
      long result=inflaterSource.read(sink,byteCount,deadline);
      if (result != -1) {
        updateCrc(sink,offset,result);
        return result;
      }
      section=SECTION_TRAILER;
    }
    if (section == SECTION_TRAILER) {
      consumeTrailer(deadline);
      section=SECTION_DONE;
    }
    return -1;
  }
  public void consumeHeader(  Deadline deadline) throws IOException {
    require(10,deadline);
    byte flags=buffer.byteAt(3);
    boolean fhcrc=((flags >> FHCRC) & 1) == 1;
    if (fhcrc)     updateCrc(buffer,0,10);
    short id1id2=buffer.readShort();
    checkEqual("ID1ID2",(short)0x1f8b,id1id2);
    buffer.skip(8);
    if (((flags >> FEXTRA) & 1) == 1) {
      require(2,deadline);
      if (fhcrc)       updateCrc(buffer,0,2);
      int xlen=buffer.readShortLe() & 0xffff;
      require(xlen,deadline);
      if (fhcrc)       updateCrc(buffer,0,xlen);
      buffer.skip(xlen);
    }
    if (((flags >> FNAME) & 1) == 1) {
      long index=seek((byte)0,deadline);
      if (fhcrc)       updateCrc(buffer,0,index + 1);
      buffer.skip(index + 1);
    }
    if (((flags >> FCOMMENT) & 1) == 1) {
      long index=seek((byte)0,deadline);
      if (fhcrc)       updateCrc(buffer,0,index + 1);
      buffer.skip(index + 1);
    }
    if (fhcrc) {
      checkEqual("FHCRC",buffer.readShortLe(),(short)crc.getValue());
      crc.reset();
    }
  }
  public void consumeTrailer(  Deadline deadline) throws IOException {
    require(8,deadline);
    checkEqual("CRC",buffer.readIntLe(),(int)crc.getValue());
    checkEqual("ISIZE",buffer.readIntLe(),inflater.getTotalOut());
  }
  @Override public void close(  Deadline deadline) throws IOException {
    inflaterSource.close(deadline);
  }
  /** 
 * Updates the CRC with the given bytes. 
 */
  public void updateCrc(  OkBuffer buffer,  long offset,  long byteCount){
    for (Segment s=buffer.head; byteCount > 0; s=s.next) {
      int segmentByteCount=s.limit - s.pos;
      if (offset < segmentByteCount) {
        int toUpdate=(int)Math.min(byteCount,segmentByteCount - offset);
        crc.update(s.data,(int)(s.pos + offset),toUpdate);
        byteCount-=toUpdate;
      }
      offset-=segmentByteCount;
    }
  }
  /** 
 * Fills the buffer with at least  {@code byteCount} bytes. 
 */
  public void require(  int byteCount,  Deadline deadline) throws IOException {
    while (buffer.byteCount < byteCount) {
      if (source.read(buffer,Segment.SIZE,deadline) == -1)       throw new EOFException();
    }
  }
  /** 
 * Returns the next index of  {@code b}, reading data into the buffer as necessary. 
 */
  public long seek(  byte b,  Deadline deadline) throws IOException {
    long start=0;
    long index;
    while ((index=buffer.indexOf(b,start)) == -1) {
      start=buffer.byteCount;
      if (source.read(buffer,Segment.SIZE,deadline) == -1)       throw new EOFException();
    }
    return index;
  }
  public void checkEqual(  String name,  int expected,  int actual) throws IOException {
    if (actual != expected) {
      throw new IOException(String.format("%s: actual %#08x != expected %#08x",name,actual,expected));
    }
  }
  public GzipSource(){
  }
}
