package Util;
import umontreal.iro.lecuyer.rng.RandomStream;

 
/**
 * Title:        <p>
 * Description:  <p>
 * Copyright:    Copyright (c) <p>
 * Company:      <p>
 * @author
 * @version 1.0
 */
 

public class Rand1  {
 



   public static double uniform (RandomStream s, double a, double b)  {
      return a + ((b-a) * s.nextDouble());
   } 



   public static double expon (RandomStream s, double mean)  {
      return (-mean * Math.log (1.0 - s.nextDouble()));
   } 



   public static double erlang (RandomStream s, int k, double mu)  {
      double x = 0.0;
      for (int i=0;  i<k;  i++)  x += expon (s, mu);
      return x;
   } 



   public static double weibull (RandomStream s, 
                                 double alpha, double lambda)  {
      if (!((alpha > 0.0) && (lambda > 0))) error (6);
      return Math.pow ((-Math.log (1.0 - s.nextDouble()) / lambda), 
                       (1.0 / alpha));
   } 



   public static double normalDist (double x)  {
      final double Racinedeux = 1.4142135623730950488;
      final double racineunsurpi = 0.56418958354775628694;

      final double p10 = 2.4266795523053175e2;
      final double p11 = 2.1979261618294152e1;
      final double p12 = 6.9963834886191355;
      final double p13 = -3.5609843701815385e-2;

      final double p20 = 3.004592610201616005e2;
      final double p21 = 4.519189537118729422e2;
      final double p22 = 3.393208167343436870e2;
      final double p23 = 1.529892850469404039e2;
      final double p24 = 4.316222722205673530e1;
      final double p25 = 7.211758250883093659e0;
      final double p26 = 5.641955174789739711e-1;
      final double p27 = -1.368648573827167067e-7;

      final double p30 = -2.99610707703542174e-3;
      final double p31 = -4.94730910623250734e-2;
      final double p32 = -2.26956593539686930e-1;
      final double p33 = -2.78661308609647788e-1;
      final double p34 = -2.23192459734184686e-2;

      final double q10 = 2.1505887586986120e2;
      final double q11 = 9.1164905404514901e1;
      final double q12 = 1.5082797630407787e1;
      final double q13 = 1.0;

      final double q20 = 3.004592609569832933e2;
      final double q21 = 7.909509253278980272e2;
      final double q22 = 9.313540948506096211e2;
      final double q23 = 6.389802644656311665e2;
      final double q24 = 2.775854447439876434e2;
      final double q25 = 7.700015293522947295e1;
      final double q26 = 1.278272731962942351e1;
      final double q27 = 1.0;

      final double q30 = 1.06209230528467918e-2;
      final double q31 = 1.91308926107829841e-1;
      final double q32 = 1.05167510706793207e0;
      final double q33 = 1.98733201817135256e0;
      final double q34 = 1.0;

      final double xasymp = 100.0;

      double Ycarre, unsurY2, Y, R, erf;

      if (x < -xasymp)
	 return 0.0;
      if (x > xasymp)
	 return 1.0;

      if (x < 0.0)
         return 1.0 - normalDist (-x);

      Y = x / Racinedeux;
      Ycarre = x * x / 2.0;
      if (Y < 0.447) {
	 R = (p10 + Ycarre * (p11 + Ycarre * (p12 + Ycarre * p13))) /
	    (q10 + Ycarre * (q11 + Ycarre * (q12 + Ycarre * q13)));
	 erf = Y * R;
      } else {
	 if (Y <= 4.0) {
	    R = (p20 + Y * (p21 + Y * (p22 + Y * (p23 + Y * (p24 + Y * (p25 +
			      Y * (p26 + Y * p27))))))) / (q20 + Y * (q21 +
		  Y * (q22 + Y * (q23 + Y * (q24 + Y * (q25 + Y * (q26 +
				 Y * q27)))))));
	    erf = 1.0 - Math.exp (-Ycarre) * R;
	 } else {
	    unsurY2 = 1.0 / Ycarre;
	    R = (p30 + unsurY2 * (p31 + unsurY2 * (p32 + unsurY2 *
		     (p33 + unsurY2 * p34)))) / (q30 + unsurY2 *
	       (q31 + unsurY2 * (q32 + unsurY2 * (q33 + unsurY2 * q34))));
	    erf = 1.0 - (Math.exp (-Ycarre) / Y) * (racineunsurpi + R / Ycarre);
	 }
      }
      return ((1.0 + erf) / 2.0);
    } 



   public static double invNormalDist (double u)  {
   // Utilise l'inversion et une approximation rationnelle donnant environ 7  *)
   // decimales de precision pour 1.0E-20 < U < 1.0 - 1.0E-20.                *)
   // Ref. : Kennedy and Gentle, "Statistical Computing", Dekker, 1980, p.95. *)
        double P0 = -0.322232431088,     Q0 = 0.0993484626060;
        double P1 = -1.000000000000,     Q1 = 0.588581570495;
        double P2 = -0.342242088547,     Q2 = 0.531103462366;
        double P3 = -0.0204231210245,    Q3 = 0.103537752850;
        double P4 = -0.0000453642210148, Q4 = 0.0038560700634;
        double Y, Z;

        if  (u > 0.5)  Y = Math.sqrt(-2.0 * Math.log(1.0 - u));
        else Y = Math.sqrt(-2.0 * Math.log(u));

        Z = Y + (((( Y * P4 + P3 ) * Y + P2 ) * Y + P1 ) * Y + P0 ) /
                  (((( Y * Q4 + Q3 ) * Y + Q2 ) * Y + Q1 ) * Y + Q0 );
        if (u < 0.5)  Z = -Z;

        return Z;
    } 



   public static double normal (RandomStream s, double mean, double sdev)  {
      if (sdev < 0.0 )  error (8);
      return mean + sdev * invNormalDist (s.nextDouble());
      } 



   public static double invStudentDist (int n, double u)  {
        double limit = 1.0E-20;
        double a, b, c, d, e, p, t, x, y;
        double pi = Math.PI;    
        e = (double) n;
        if (u > 0.5) p = 2.0*(1.0-u);  else p = 2.0*u;
        if (n < 1) error (10);
        if (p <= 2.0 * limit) error (9);

        if (n==1) t = Math.abs (Math.cos (pi*p/2.0) / Math.sin (pi*p/2.0));
        else if (n==2) t = Math.sqrt (2.0/(p*(2.0-p))-2.0);
        else  {                  // We have n > 2
            a = 1. / ( e - 0.5 );
            b = 48. / ( a * a );
            c = ( ( 20700. /b * a - 98.) * a - 16.) * a + 96.36;
            d = e * Math.sqrt (a * pi/2.) *((94.5/(b + c) - 3.) / b + 1.);
            y = Math.pow ((d * p), (2.0 / e));
            if (y > (a + 0.05)) {
                if (p == 1.0)
                    x = 0.0;   // Permet de sauver un appel a InvNormalDist.
                else
                    x = invNormalDist ( p * 0.5 );
                y = x * x;
                if (n < 5)
                     c = c + 0.3 * ( e - 4.5 ) * ( x + 0.6 );

                c = ( ( (0.05 * d * x - 5.) * x - 7. ) * x - 2. ) * x + b + c;
                y = ( ( ( ( ( 0.4 * y + 6.3 ) * y + 36. ) * y + 94.5 ) /
                      c - y - 3. ) / b + 1. ) * x;
                y = a * ( y * y );
                if (y > 0.002)
                    y = Math.exp( y ) - 1.0;
                else
                    y = ( 0.5 * y * y ) + y;
            }
            else
                y = ( ( 1. / ( ( ( e + 6. ) / ( e * y ) - 0.089 * d - 0.822 ) *
                     ( e + 2. ) * 3. ) + 0.5 / ( e + 4. ) ) * y - 1. ) *
                    ( e + 1. ) / ( e + 2. ) + 1. / y;

            t = Math.sqrt ( e * y );
        }
        if (u < 0.5) return -t;  else  return t;
    } 



   public static double student (RandomStream s, int n)  {
      if  (n < 1)  error (11);
      // double u = s.nextDouble();
      return invStudentDist (n, s.nextDouble());
   } 


 

  private static void error (int err) {}
  // Utiliser directement la classe  Error  de java a la 
  // place de cette methode.
} 

