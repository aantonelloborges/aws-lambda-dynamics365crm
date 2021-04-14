package com.accenture.MSDynamics.entities;

public class AuthorityEntity {

	public String token_type;
	public String expires_in;
	public String ext_expires_in;
	public String expires_on;
	public String not_before;
	public String resource;
	public String access_token;

	public AuthorityEntity(){}
	// public AuthorityEntity(String token_type, String expires_in, String ext_expires_in, String expires_on,
	// 		String not_before, String resource, String access_token) {
	// 	this.token_type = token_type;
	// 	this.expires_in = expires_in;
	// 	this.ext_expires_in = ext_expires_in;
	// 	this.expires_on = expires_on;
	// 	this.not_before = not_before;
	// 	this.resource = resource;
	// 	this.access_token = access_token;
	// }

	// @Override
	// public String toString() {
	// 	return new StringBuilder().append("AuthorityEntity{").append("token_type: ").append(token_type)
	// 			.append(", expires_in: ").append(expires_in).append(", ext_expires_in: ").append(ext_expires_in)
	// 			.append(", expires_on: ").append(expires_on).append(", not_before: ").append(not_before)
	// 			.append(", resource: ").append(resource).append(", access_token: ").append(access_token).append("}")
	// 			.toString();
	// }

	// public String getToken() {
	// 	return this.access_token;
	// }
}