/*
 * Copyright � 1997 - 1999 IBM Corporation.
 * 
 * Redistribution and use in source (source code) and binary (object code)
 * forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * 1. Redistributed source code must retain the above copyright notice, this
 * list of conditions and the disclaimer below.
 * 2. Redistributed object code must reproduce the above copyright notice,
 * this list of conditions and the disclaimer below in the documentation
 * and/or other materials provided with the distribution.
 * 3. The name of IBM may not be used to endorse or promote products derived
 * from this software or in any other form without specific prior written
 * permission from IBM.
 * 4. Redistribution of any modified code must be labeled "Code derived from
 * the original OpenCard Framework".
 * 
 * THIS SOFTWARE IS PROVIDED BY IBM "AS IS" FREE OF CHARGE. IBM SHALL NOT BE
 * LIABLE FOR INFRINGEMENTS OF THIRD PARTIES RIGHTS BASED ON THIS SOFTWARE.  ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IBM DOES NOT WARRANT THAT THE FUNCTIONS CONTAINED IN THIS
 * SOFTWARE WILL MEET THE USER'S REQUIREMENTS OR THAT THE OPERATION OF IT WILL
 * BE UNINTERRUPTED OR ERROR-FREE.  IN NO EVENT, UNLESS REQUIRED BY APPLICABLE
 * LAW, SHALL IBM BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.  ALSO, IBM IS UNDER NO OBLIGATION
 * TO MAINTAIN, CORRECT, UPDATE, CHANGE, MODIFY, OR OTHERWISE SUPPORT THIS
 * SOFTWARE.
 */

package com.ibm.opencard.isofs;


import opencard.core.service.CardServiceScheduler;
import opencard.core.service.SmartCard;

import com.ibm.opencard.service.MFCCardServiceParameter;


/**
 * Encapsulation of the constructor arguments of <tt>MFCFileAccess</tt>.
 * The number of arguments to a card service is rather high. To simplify the
 * construction, this object encapsulates all parameters required for the
 * <i>File Access</i> service, except an optional pre-allocated channel.
 * Every parameter can be set explicitly by a method invocation. Access
 * to the parameters stored is provided only within this package.
 * <br>
 * The file access card service <tt>MFCFileAccess</tt> is derived from the
 * generic card service <tt>MFCCardService</tt>. Consequently, the parameters
 * are derived from the generic parameters, which are still required to
 * invoke the base class constructor.
 *
 * @version $Id: MFCFileParameter.java,v 1.8 1998/09/14 11:51:32 cvsusers Exp $
 *
 * @author Roland Weber (rolweber@de.ibm.com)
 *
 * @see MFCFileAccess
 * @see com.ibm.opencard.service.MFCCardServiceParameter
 */
public class MFCFileParameter extends MFCCardServiceParameter
{
  /** The file access implementation */
  MFCFileAccessImpl fa_impl = null;


  // construction /////////////////////////////////////////////////////////////

  /**
   * Creates new parameters, initialized with <tt>null</tt>.
   * All parameters required for instantiating a <tt>MFCFileAccess</tt>,
   * including those for the base class, must be set explicitly afterwards.
   *
   * @see MFCFileAccess
   */
  public MFCFileParameter()
  {
    ;
  }


  /**
   * Creates new parameters, the standard ones already set.
   * In the first version of OCF, all card services had the same constructor
   * arguments. This constructor expects exactly those arguments, and stores
   * them. The remaining arguments required for instantiating a
   * <tt>MFCFileAccess</tt>, including those for the base class, have to
   * be set explicitly afterwards.
   *
   * @param scheduler   where to allocate channels
   * @param smartcard   the smartcard to contact
   * @param blocking    whether channel allocation can block
   *
   * @see MFCFileAccess
   * @see MFCCardServiceParameter
   */
  public MFCFileParameter(CardServiceScheduler scheduler,
                          SmartCard            smartcard,
                          boolean              blocking )
  {
    super(scheduler, smartcard, blocking);
  }


  // access ///////////////////////////////////////////////////////////////////

  /**
   * Sets the file access service implementation to use.
   * All file access services for a specific card OS version share a single
   * file access service implementation, which is responsible for creating
   * commands and interpreting the smartcard's responses.
   *
   * @param faimpl   the file access implementation to use
   */
  public final void setFileAccessImpl(MFCFileAccessImpl faimpl)
  {
    fa_impl = faimpl;
  }

} // class MFCFileParameter
