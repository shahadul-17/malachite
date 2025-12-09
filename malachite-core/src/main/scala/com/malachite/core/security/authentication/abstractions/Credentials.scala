package com.malachite.core.security.authentication.abstractions

import com.malachite.core.text.JsonSerializable
import tools.jackson.databind.annotation.JsonDeserialize

@JsonDeserialize(as = classOf[CredentialsImpl])
trait Credentials extends JsonSerializable {
	val username: String
	val password: String

	override def hashCode: Int = s"$username::$password".hashCode

	override def equals(obj: Any): Boolean = obj match {
		case credentials: Credentials =>
			credentials.username == username && credentials.password == password
		case _ => false
	}
}

object Credentials {
	def apply(username: String,
			  password: String,
			 ): Credentials
		= new CredentialsImpl(username, password)
}

private class CredentialsImpl(override val username: String,
							  override val password: String,
							 ) extends Credentials
