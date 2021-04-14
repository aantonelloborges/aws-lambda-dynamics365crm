package com.accenture.MSDynamics.entities;

public class ContactEntity {

	public String contactid;
	public String fullname;
	public String telephone1;
	
	public ContactEntity(){}

	@Override
	public String toString() {
		return new StringBuilder().append("AuthorityEntity{")
				.append("contactid: ").append(contactid)
				.append(", fullname: ").append(fullname)
				.append(", telephone1: ").append(telephone1)
				.append("}")
				.toString();
	}
}