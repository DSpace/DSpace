package cz.cuni.mff.ufal.dspace.rest;

import javax.ws.rs.NameBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by okosarko on 13.10.15.
 */

@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@interface Disable {}
