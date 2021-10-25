package com.samourai.wallet.segwit.bech32;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Locale;

import com.samourai.wallet.util.Triple;

public class Bech32 {

  private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
  private static final int BECH32M_CONST = 0x2bc830a3;

  public static final int BECH32 = 1;
  public static final int BECH32M = 2;

  public static String bech32Encode(String hrp, byte[] data, int spec) {

      byte[] chk = createChecksum(hrp.getBytes(), data, spec);
      byte[] combined = new byte[chk.length + data.length];

      System.arraycopy(data, 0, combined, 0, data.length);
      System.arraycopy(chk, 0, combined, data.length, chk.length);

      byte[] xlat = new byte[combined.length];
      for(int i = 0; i < combined.length; i++)   {
          xlat[i] = (byte)CHARSET.charAt(combined[i]);
      }

      byte[] ret = new byte[hrp.getBytes().length + xlat.length + 1];
      System.arraycopy(hrp.getBytes(), 0, ret, 0, hrp.getBytes().length);
      System.arraycopy(new byte[] { 0x31 }, 0, ret, hrp.getBytes().length, 1);
      System.arraycopy(xlat, 0, ret, hrp.getBytes().length + 1, xlat.length);

      return new String(ret);
  }

  public static Triple<String, byte[], Integer> bech32Decode(String bech) {

      byte[] buffer = bech.getBytes();
      for(byte b : buffer)   {
          if(b < (byte)0x21 || b > (byte)0x7e)    {
              return null;
          }
      }

      if(!bech.equals(bech.toLowerCase(Locale.ROOT)) && !bech.equals(bech.toUpperCase(Locale.ROOT)))  {
          return null;
      }

      bech = bech.toLowerCase();
      int pos = bech.lastIndexOf("1");
      if(pos < 1)    {
          return null;
      }
      else if(pos + 7 > bech.length())    {
          return null;
      }
      else if(bech.length() < 8)    {
          return null;
      }
      else if(bech.length() > 90)    {
          return null;
      }
      else    {
          ;
      }

      String s = bech.substring(pos + 1);
      for(int i = 0; i < s.length(); i++) {
          if(CHARSET.indexOf(s.charAt(i)) == -1)    {
              return null;
          }
      }

      byte[] hrp = bech.substring(0, pos).getBytes();

      byte[] data = new byte[bech.length() - pos - 1];
      for(int j = 0, i = pos + 1; i < bech.length(); i++, j++) {
          data[j] = (byte)CHARSET.indexOf(bech.charAt(i));
      }

      int spec = verifyChecksum(hrp, data);
      if (spec == 0) {
          return null;
      }

      byte[] ret = new byte[data.length - 6];
      System.arraycopy(data, 0, ret, 0, data.length - 6);

      return Triple.of(new String(hrp), ret, spec);
  }

  private static int polymod(byte[] values)  {

      final int[] GENERATORS = { 0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3 };

      int chk = 1;

      for(byte b : values)   {
          byte top = (byte)(chk >> 0x19);
          chk = b ^ ((chk & 0x1ffffff) << 5);
          for(int i = 0; i < 5; i++)   {
              chk ^= ((top >> i) & 1) != 0 ? GENERATORS[i] : 0;
          }
      }

      return chk;
  }

  private static byte[] hrpExpand(byte[] hrp) {

      byte[] buf1 = new byte[hrp.length];
      byte[] buf2 = new byte[hrp.length];
      byte[] mid = new byte[1];

      for (int i = 0; i < hrp.length; i++)   {
          buf1[i] = (byte)(hrp[i] >> 5);
      }
      mid[0] = (byte)0x00;
      for (int i = 0; i < hrp.length; i++)   {
          buf2[i] = (byte)(hrp[i] & 0x1f);
      }

      byte[] ret = new byte[(hrp.length * 2) + 1];
      System.arraycopy(buf1, 0, ret, 0, buf1.length);
      System.arraycopy(mid, 0, ret, buf1.length, mid.length);
      System.arraycopy(buf2, 0, ret, buf1.length + mid.length, buf2.length);

      return ret;
  }

  private static int verifyChecksum(byte[] hrp, byte[] data) {

      byte[] exp = hrpExpand(hrp);

      byte[] values = new byte[exp.length + data.length];
      System.arraycopy(exp, 0, values, 0, exp.length);
      System.arraycopy(data, 0, values, exp.length, data.length);

      switch (polymod(values)) {
        case 1:
          return BECH32;
        case BECH32M_CONST:
          return BECH32M;
        default:
          return 0;
      }

  }

  private static byte[] createChecksum(byte[] hrp, byte[] data, int spec)  {

      final byte[] zeroes = new byte[] { (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 };
      byte[] expanded = hrpExpand(hrp);
      byte[] values = new byte[zeroes.length + expanded.length + data.length];

      System.arraycopy(expanded, 0, values, 0, expanded.length);
      System.arraycopy(data, 0, values, expanded.length, data.length);
      System.arraycopy(zeroes, 0, values, expanded.length + data.length, zeroes.length);

      int _const = (spec == Bech32.BECH32M ? BECH32M_CONST : Bech32.BECH32);

      int polymod = polymod(values) ^ _const;
      byte[] ret = new byte[6];
      for(int i = 0; i < ret.length; i++)   {
          ret[i] = (byte)((polymod >> 5 * (5 - i)) & 0x1f);
      }

      return ret;
  }

}
