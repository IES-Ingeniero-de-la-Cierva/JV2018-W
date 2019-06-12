/**  
 * Proyecto: Juego de la vida.
 *  Resuelve todos los aspectos del almacenamiento del
 *  DTO Simulacion utilizando base de datos mySQL.
 *  Colabora en el patron Fachada.
 *  @since: prototipo2.2
 *  @source: SimulacionesDAO.java  
 *  @version: 2.2 - 2019/06/12  
 *  @author: arm - Antonio Ramírez Márquez
 *  @author: Fran Arce
 *  @author: VictorJLucas
 *  @author: themajoser
 */
 
package accesoDatos.mySql;
 
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
 
import java.sql.Connection;
 
import accesoDatos.DatosException;
import accesoDatos.OperacionesDAO;
import config.Configuracion;
import modelo.ClaveAcceso;
import modelo.Correo;
import modelo.DireccionPostal;
import modelo.ModeloException;
import modelo.Mundo;
import modelo.Nif;
import modelo.Simulacion;
import modelo.Simulacion.EstadoSimulacion;
import modelo.Usuario;
import modelo.Usuario.RolUsuario;
import util.Fecha;
import util.Formato;
 
public class SimulacionesDAO implements OperacionesDAO {
    
	
    // Singleton
    private static SimulacionesDAO instance = null;
    private Connection db;
    private Statement stSimulaciones;
    private ResultSet rsSimulaciones;
    private DefaultTableModel tmSimulaciones;     // Tabla del resultado de la consulta.
    private ArrayList<Simulacion> bufferSimulaciones;     
 
    /**
     *  Método de acceso a la instancia única.
     *  Si no existe la crea invocando al constructor interno.
     *  Utiliza inicialización diferida de la instancia única.
     *  @return instancia
     */
    public static SimulacionesDAO getInstance() {
   	 if (instance == null) {
   		 try {
			instance = new SimulacionesDAO();
		} catch (DatosException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
   	 }
   	 return instance;
    }
 
    /**
     * Constructor de uso interno.
     * @throws DatosException 
     */
    private SimulacionesDAO() throws DatosException {
   		 inicializar();
   		 try {
			alta(new Simulacion());
		} catch (ModeloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
 
    /**
     * Inicializa el DAO, detecta si existen las tablas de datos capturando la
     * excepción SQLException.
     * @throws SQLException  
     */
    private void inicializar() {
   	 db = Conexion.getDB();
   	 try {
   		 crearTablaSimulaciones();
   		 stSimulaciones = db.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
   	 // Crea el tableModel y el buffer de objetos para usuarios.
   	 tmSimulaciones = new DefaultTableModel();
   	 bufferSimulaciones = new ArrayList<Simulacion>();  
    }
 
    /**
     * Crea la tabla de usuarios en la base de datos.
     * @throws SQLException  
     */
    private void crearTablaSimulaciones() throws SQLException {
   	 Statement s = db.createStatement();
   	 s.executeUpdate("CREATE TABLE IF NOT EXISTS simulaciones("  
   			 + "`usuario` VARCHAR(45) NOT NULL,"
   			 + "fecha DATE,"
   			 + "mundo VARCHAR(45) NOT NULL,"
   			 + "ciclos CHAR(10) NOT NULL,"  
   			 + "estado VARCHAR(20) NOT NULL,"  
   			 + "PRIMARY KEY (`usuario`, `fecha`))");
   			 
    }
 
    // MÉTODOS DAO Usuarios
 
    /**
     * Obtiene el usuario buscado dado un objeto.  
     * Si no existe devuelve null.
     * @param Usuario a obtener.
     * @return (Usuario) buscado.  
     */    
    public Simulacion obtener(Object obj) {
   	 assert obj != null;
   	 return obtener(((Simulacion) obj).getId());
    }
 
    /**
     * Obtiene el usuario buscado dado su id, el nif o el correo.  
     * Si no existe devuelve null.
     * @param idUsr del usuario a obtener.
     * @return (Usuario) buscado.  
     */    
    @Override
    public Simulacion obtener(String id) {   	 
   	 assert id != null;
   	 assert !id.equals("");
   	 assert !id.equals(" ");
   	 
   	 try {
		ejecutarConsulta(id);
	} catch (ModeloException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
 
   	 // Establece columnas y etiquetas.
   	 establecerColumnasModelo();
 
   	 // Borrado previo de filas.
   	 borrarFilasModelo();
 
   	 // Volcado desde el resulSet.
   	 rellenarFilasModelo();
 
   	 // Actualiza buffer de objetos.
   	 sincronizarBufferUsuarios();
 
   	 if (bufferSimulaciones.size() > 0) {
   		 return (Simulacion) bufferSimulaciones.get(0);
   	 }
   	 return null;
    }
 
    /**
     * Determina el idUsr recibido y ejecuta la consulta.
     * Los resultados quedan en el ResultSet
     * @param idUsr
     * @throws ModeloException 
     */
    private void ejecutarConsulta(String idSimul) throws ModeloException {
   	 try {
   		Simulacion simul= obtener(idSimul);
   		String id =simul.getUsr().getId();
   		String fecha=fechaParaSimulacion(simul.getFecha());
   		
   		 rsSimulaciones = stSimulaciones.executeQuery("SELECT * FROM simulaciones "
                	+ "WHERE usuario='"+id+"' AND fecha='"+fecha+"'");
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
    }
    /**
     * Adapta el formato de la fecha para consultas mySql.
     * @param fecha
     * @return String
     */
    private static String fechaParaSimulacion(Fecha fecha) {
		
			String año=fecha.toStringMarcaTiempo().substring(0,4);
			String mes=fecha.toStringMarcaTiempo().substring(4,6);
			String dia=fecha.toStringMarcaTiempo().substring(6,8);
			
		return año+"-"+mes+"-"+dia ;
			
    }
 
    /**
     * Crea las columnas del TableModel a partir de los metadatos del ResultSet
     * de una consulta a base de dato
     */
    private void establecerColumnasModelo() {
   	 try {
   		 // Obtiene metadatos.
   		 ResultSetMetaData metaDatos = this.rsSimulaciones.getMetaData();
 
   		 // Número total de columnas.
   		 int numCol = metaDatos.getColumnCount();
 
   		 // Etiqueta de cada columna.
   		 Object[] etiquetas = new Object[numCol];
   		 for (int i = 0; i < numCol; i++) {
   			 etiquetas[i] = metaDatos.getColumnLabel(i + 1);
   		 }
 
   		 // Incorpora array de etiquetas en el TableModel.
   		 ((DefaultTableModel) this.tmSimulaciones).setColumnIdentifiers(etiquetas);
   	 } 
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
    }
 
    /**
     * Borra todas las filas del TableModel
     * @param tm - El TableModel a vaciar
     */
    private void borrarFilasModelo() {
   	 while (this.tmSimulaciones.getRowCount() > 0)
   		 ((DefaultTableModel) this.tmSimulaciones).removeRow(0);
    }
 
    /**
     * Replica en el TableModel las filas del ResultSet
     */
    private void rellenarFilasModelo() {
   	 Object[] datosFila = new Object[this.tmSimulaciones.getColumnCount()];
   	 // Para cada fila en el ResultSet de la consulta.
   	 try {
   		 while (rsSimulaciones.next()) {
   			 // Se replica y añade la fila en el TableModel.
   			 for (int i = 0; i < this.tmSimulaciones.getColumnCount(); i++) {
   				 datosFila[i] = this.rsSimulaciones.getObject(i + 1);
   			 }
   			 ((DefaultTableModel) this.tmSimulaciones).addRow(datosFila);
   		 }
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
    }
 
    /**
     * Regenera lista de los objetos procesando el tableModel.  
     */    
    private void sincronizarBufferUsuarios() {
   	 bufferSimulaciones.clear();
   	 for (int i = 0; i < tmSimulaciones.getRowCount(); i++) {
   		 Usuario usr = new Usuario((Usuario) tmSimulaciones.getValueAt(i, 0));
   		 Fecha fecha = new Fecha((java.sql.Date)tmSimulaciones.getValueAt(i, 1));
   		 Mundo mundo = new Mundo((Mundo)tmSimulaciones.getValueAt(i, 2));
   		 int ciclos = (int) tmSimulaciones.getValueAt(i, 3);
   		 EstadoSimulacion estado = null;
   		 switch ((String) tmSimulaciones.getValueAt(i, 4)) {
   		 case "PREPARADA":   
   			 estado = EstadoSimulacion.PREPARADA;
   			 break;
   		 case "INICIADA":
   			 estado = EstadoSimulacion.INICIADA;
   			 break;
   		 case "COMPLETADA":
   			 estado = EstadoSimulacion.COMPLETADA;
   			 break;
   		 }    
   		 // Genera y guarda objeto
   		 bufferSimulaciones.add(new Simulacion(usr, fecha, mundo, ciclos, estado));
   	 }
    }
 
    public ArrayList<Simulacion> obtenerTodasMismoUsr(String idUsr) {
   	 assert idUsr != null;
   	 assert !idUsr.equals("");
   	 assert !idUsr.equals(" ");
   	 
   	 try {
   		 rsSimulaciones = stSimulaciones.executeQuery("SELECT * FROM simulaciones WHERE usuario = '" + idUsr + "'");
   		 this.establecerColumnasModelo();

   		 this.borrarFilasModelo();

   		 this.rellenarFilasModelo();

   		 this.sincronizarBufferUsuarios();   
   	 
   	 } catch (SQLException e) {
   		 e.printStackTrace();
   	 }
   	 
   	 return this.bufferSimulaciones;
    }

    /**
     * Da de alta un nuevo usuario en la base de datos.
     * @param usr, el objeto a dar de alta.
     * @throws DatosException  
     */    
    @Override
    public void alta(Object obj) throws DatosException {    
   	 assert obj != null;
   	 Simulacion simulNuevo = (Simulacion) obj;
   	 Simulacion simulObtenido = obtener(simulNuevo.getId());
   	 if (simulObtenido == null) {
   		 try {
   			 almacenar(simulNuevo);
   			 return;
   		 }  
   		 catch (SQLException e) {
   			 e.printStackTrace();
   		 } 		 
   	 }
   	 throw new DatosException("SimulacionesDAO.alta:" + simulNuevo.getId() +" Ya existe.");
    }
 
    /**
     * almacena usuario en la base de datos.
     * @param usr, el objeto a procesar.
     * @throws SQLException   
     */
    private void almacenar(Simulacion simul) throws SQLException {
   	 assert simul != null;
   	 // Se realiza la consulta y los resultados quedan en el ResultSet actualizable.
   	 this.rsSimulaciones = this.stSimulaciones.executeQuery("SELECT * FROM simulaciones");
   	 this.rsSimulaciones.moveToInsertRow();
   	 this.rsSimulaciones.updateObject("usr", simul.getUsr());
   	 this.rsSimulaciones.updateObject("fecha", simul.getFecha());
   	 this.rsSimulaciones.updateObject("mundo", simul.getMundo());
   	 this.rsSimulaciones.updateInt("ciclos", simul.getCiclos());
   	 this.rsSimulaciones.updateString("estado", simul.toString());
   	 this.rsSimulaciones.insertRow();
   	 this.rsSimulaciones.beforeFirst();    
    }
 
    /**
     * Da de baja un usuario de la base de datos.
     * @param idUsr, id del usuario a dar de baja.
     * @throws DatosException
     */
    @Override
    public Simulacion baja(String id) throws DatosException  {
   	 assert id != null;
   	 assert !id.equals("cutarDemoSimulacion() {\n" + 
   	 		"		Simulacion demo = new Simulacion(sesionUsr.getUsr(),\n" + 
   	 		"				new Fecha(), \n" + 
   	 		"				datos.obtenerMundo(Configuracion.get().getProperty(\"mundo.nombrePredeterminado\")),\n" + 
   	 		"				Integer.parseInt(Configuracion.get().getProperty(\"simulacion.ciclosPredeterminados\")),\n" + 
   	 		"				EstadoSimulacion.PREPARADA);");
   	 assert !id.equals(" ");
   	 
   	 Simulacion simul = obtener(id);
		String id_simu =simul.getUsr().getId();
		String fecha=fechaParaSimulacion(simul.getFecha());
		
		 
   	 if (simul != null) {
   		 try {
   			 this.bufferSimulaciones.remove(simul);
   			 this.stSimulaciones.executeQuery("DELETE FROM simulaciones WHERE usuario='"+id_simu+"' AND fecha='"+fecha+"'");
   		 }  
   		 catch (SQLException e) {
   			 e.printStackTrace();
   		 }
   		 return simul;
   	 }
   	 throw new DatosException("BAJA: la Simulacion " + id + " no existe...");
    }
 
    /**
     * Modifica un usuario de la base de datos.
     * @param usr, objeto Usuario con los valores a cambiar.
     * @throws DatosException  
     */
    @Override
    public void actualizar(Object obj) throws DatosException {
		assert obj != null;
		Simulacion simuActualizado = (Simulacion) obj;
		Simulacion simuPrevio = (Simulacion) obtener(simuActualizado.getId());
		String fecha=fechaParaSimulacion(simuActualizado.getFecha());
		if (simuPrevio != null) {
			try {
				this.bufferSimulaciones.remove(simuPrevio);
				this.bufferSimulaciones.add(simuActualizado);
				this.stSimulaciones.executeQuery("UPDATE simulaciones SET "
						+ " usuario = " + simuActualizado.getUsr() + ","
						+ " fecha = " + simuActualizado.getFecha() + ","
						+ " mundo = " + simuActualizado.getMundo() + ","
						+ " ciclos = " + simuActualizado.getCiclos() + ","
						+ " estado = " + simuActualizado.getEstado() + ","
						+ " WHEWHERE usuario='"+simuActualizado.getUsr().getId()+"' AND fecha='"+fecha+"'"
						);
				return;
			} 
			catch (SQLException e) {
				e.printStackTrace();
			}
		}
		throw new DatosException("Actualizar: "+ simuActualizado.getId() + " no existe.");
    }
    
    /** Devuelve listado completo de usuarios.
     *  @return texto con el volcado de todos los usuarios.
     */
    @Override
    public String listarDatos() {
   	 return obtenerTodos().toString();
    }
 
    /**
     * Obtiene todos los usuarios almacenados.  
     * Si no hay resultados devuelve null.
     * @param idUsr a obtener.
     * @return (DefaultTableModel) result obtenido.
     * @throws SQLException  
     */    
    public ArrayList<Simulacion> obtenerTodos() {
   	 try {
   		 // Se realiza la consulta y los resultados quedan en el ResultSet
   		 this.rsSimulaciones = stSimulaciones.executeQuery("SELECT * FROM simulaciones");
 
   		 // Establece columnas y etiquetas
   		 this.establecerColumnasModelo();
 
   		 // Borrado previo de filas
   		 this.borrarFilasModelo();
 
   		 // Volcado desde el resulSet
   		 this.rellenarFilasModelo();
 
   		 // Actualiza buffer de objetos.
   		 this.sincronizarBufferUsuarios();    
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
   	 return this.bufferSimulaciones;
    }
 
    @Override
    public void borrarTodo() {
   	 bufferSimulaciones.clear();
   	 try {
   		 this.stSimulaciones.executeQuery("DELETE FROM simulaciones");
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
    }
 
    /**
     * Obtiene el listado de todos id de los objetos almacenados.
     * @return el texto con el volcado de id.
     */
    @Override
    public String listarId() {
   	 try {
   		
   		
   		 this.rsSimulaciones = stSimulaciones.executeQuery("SELECT CONCAT(usuario,':',DATE_FORMAT(fecha,'%Y%m%d%H%i%s')) FROM simulaciones");
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
   	 this.establecerColumnasModelo();
   	 this.borrarFilasModelo();
   	 this.rellenarFilasModelo();
   	 StringBuilder result = new StringBuilder();
   	 for (int i = 0; i < tmSimulaciones.getRowCount(); i++) {
   		 String id = (String) tmSimulaciones.getValueAt(i, 0);
   	 }
   	 return result.toString();
    }
 
    /**
     * Cierra conexiones.
     */
    @Override
    public void cerrar() {
   	 try {
   		 stSimulaciones.close();
   	 }  
   	 catch (SQLException e) {
   		 e.printStackTrace();
   	 }
    }

    
} // class

