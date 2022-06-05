package Backend;

import static com.twilio.rest.api.v2010.account.Call.Status.*;
import static spark.Spark.*;

import com.twilio.Twilio;
import com.twilio.http.HttpMethod;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
//import com.twilio.twiml.Say;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Say;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

public class SMSBackend {

    private static final String ACCOUNT_SID = "AC5894ad5ba3b84be844b46286a9c2ac73";
    private static final String AUTH_TOKEN = "7ec31b602bb9c8df10549e6d4530d599";
    private static final String from = "+15076097373";
    private static int Fallos=0;
    private static Call call = null;
    static ArrayList<String> Contactos = new ArrayList<String>();
    private static String usuario;
    private static String ubicacion;
    private static String numeroALlamar;

    public static void main(String[] args) {

        get("/", (req, res) -> {
            //Llamar();
            //Mensaje("AYUDA");

            return "LlegaBien";
        });

        post("/emergencia", (req, res) -> {
            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            Fallos = 0;
            System.out.println("ESTOY EN EMERGENCIA");

            // Se recibe la información enviada por la aplicacion y se declaran los valores de variables importantes
            String semiContacto;
            usuario = req.queryParams("NombreUsuario");
            ubicacion = req.queryParams("Ubicacion");
            numeroALlamar = req.queryParams("NumeroALlamar");
            int CantidadContactos = Integer.parseInt(req.queryParams("CantidadContactos"));
            System.out.println("Cantidad Cotactos" + req.queryParams("CantidadContactos"));

            // Dependiendo de la cantidad de contactos enviada por la aplicacion, se añaden sus numeros a un array
            for(int i = 0; i<CantidadContactos; i++){
                int num = i+1;
                semiContacto=req.queryParams("Contacto"+num);

                if(!semiContacto.equals("-1")) {
                    Contactos.add(req.queryParams("Contacto" + num));
                    System.out.println("CONTACTO" + num + " = " + Contactos.get(i));
                }
            }
            Mensaje(CrearMensaje_Panico());
            Llamar();
            return "message.getSid()";
        });

        post("/sms", (req, res) -> {
            String body = req.queryParams("Body");
            String to = req.queryParams("To");

            body = "¡ALERTA! La persona nombre_de_usuario se encuentra en una situación de alto peligro y necesita tu ayuda. nombre_de_usuario se encuentra en la ubicación: link_de_ubicación." +
                    "La aplicación LLEGA BIEN te manda este mensaje en automático cuando se decía que nombre_de_usuario apretó un botón de pánico. También, la aplicación ya realizó una llamada al 911, si es posible, verifica que las autoridades vayan en camino.";

      //      Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            Message message = Message.creator(
                            new PhoneNumber(to),
                            from,
                            body).create();
            return "OK";
        });


        post("/event", (req, res) -> {
//          String to = req.queryParams("CallStatus");
            String duracion = req.queryParams("CallDuration");

            System.out.println("ESTOY EN EVENT");

            if (Integer.parseInt(duracion) < 22)
                AumentarFallos();
            else
                Fallos = 0;

            System.out.println("NUMERO FALLOS: " + Fallos);
            System.out.println("Duracion: " + duracion);

            if (Fallos > 0 && Fallos < 3)
                Llamar();

            else if(Fallos == 0)
                return "200 OK";

            else {
                System.out.println("YA SE LLAMO 3 VECES");
                Fallos = 0;
                Mensaje(CrearMensaje_PoliciaNoContesta());
            }

            return "200 OK";
        });

        post("/call", (request, response) -> {
         //   Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
            String to = "+523321707532";

            String a = "Hola";
            String b = " Soy mariana";

            Say say  = new Say.Builder(a + b)
                    .build();

            VoiceResponse voiceResponse = new VoiceResponse.Builder()
                    .say(say).build();

            call = Call.creator(new PhoneNumber(to), new PhoneNumber(from),
                    new Twiml(voiceResponse.toXml())).create();

            return call.getSid();
        });

    }

    public static void Llamar(){
        System.out.println("ESTOY EN LLAMADA");
//        numeroALlamar = "+523321707532";

        Say say  = new Say.Builder(CrearTTS_Llamada())
                .build();
        VoiceResponse voiceResponse = new VoiceResponse.Builder()
                .say(say).build();
        //TODO Cambiar link de ngrok
        call =  Call.creator(new PhoneNumber(numeroALlamar), new PhoneNumber(from),
                        new com.twilio.type.Twiml(voiceResponse.toXml())).setMethod(HttpMethod.GET)
                .setStatusCallback(URI.create("https://1eb7-2806-103e-29-1d5e-b103-92db-92d1-fe29.ngrok.io/event"))
                .setStatusCallbackEvent(
                        Arrays.asList("completed"))
                .setStatusCallbackMethod(HttpMethod.POST)
                .create();
    }

    public static void Mensaje(String body) {
        for(int i = 0; i<Contactos.size(); i++) {
            Message message = Message.creator(
                    new PhoneNumber(Contactos.get(i)),
                    new PhoneNumber(from),
                    body).create();
        }
    }

    public static String CrearMensaje_Panico(){
        String respuesta = "ALERTA! La persona " + usuario + " se encuentra en una situacion de alto peligro y necesita tu ayuda. " + usuario
                + " se encuentra en la ubicacion: " + ubicacion +
                " La aplicacion LLEGA BIEN te manda este mensaje en automatico al detectar que "
                + usuario + " apreto un boton de panico. Tambien, la aplicacion ya realizo una llamada al 911, si es posible, verifica que las autoridades vayan en camino.";
        return respuesta;
    }

    public static String CrearTTS_Llamada(){
        String TTS = "La persona " + usuario + " que se encuentra en " + ubicacion + " acaba de presionar un boton de panico " +
                "en la aplicacion LLEGA BIEN y necesita ayuda inmediata de las autoridades.";
        return TTS;
    }

    public static String CrearMensaje_PoliciaNoContesta(){
        String respuesta = "La aplicacion LLEGA BIEN intento llamar en tres ocasiones al numero 911 y  no se obtuvo exito. Es muy importante que tu intentes contactar a las autoridades de inmediato.";
        return respuesta;
    }

    public static void AumentarFallos(){
            Fallos ++;
    }
}