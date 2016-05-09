package net.networkdowntime.db.viewFilter;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * A filter class to include or exclude tables based on whether they have a primary key.
 * 
 * @author Ryan.Wiles
 * 
 */
@XmlRootElement
public enum PkFilter implements Serializable {
	/**
	 * NoPK indicates that you want to filter out tables that have a primary key.
	 */
	NoPK,

	/**
	 * HasPK indicates that you want to filter out all tables that don't have a primary key.
	 */
	HasPK,

	/**
	 * All indicates that you do not want any filtering done for the primary key.
	 */
	All
}