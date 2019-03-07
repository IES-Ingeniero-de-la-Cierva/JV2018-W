/** 
 * Proyecto: Juego de la vida.
 * Clase de excepción para errores del modelo.
 * @since: prototipo1.2
 * @source: AccesoUsrException.java 
 * @version: 1.2 - 2019.03.02
 * @author: ajp
 */

package accesoUsr;

public class AccesoUsrException extends Exception {

	public AccesoUsrException(String mensaje) {
		super(mensaje);
	}

	public AccesoUsrException() {
		super();
	}
}
