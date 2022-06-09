package net.ausiasmarch.wildcart.service;

import java.util.ArrayList;
import java.util.List;
import net.ausiasmarch.wildcart.exception.ResourceNotFoundException;
import net.ausiasmarch.wildcart.exception.ResourceNotModifiedException;
import net.ausiasmarch.wildcart.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.ausiasmarch.wildcart.entity.UsuarioEntity;
import net.ausiasmarch.wildcart.exception.CannotPerformOperationException;
import net.ausiasmarch.wildcart.helper.RandomHelper;
import net.ausiasmarch.wildcart.helper.TipoUsuarioHelper;
import net.ausiasmarch.wildcart.helper.ValidationHelper;
import net.ausiasmarch.wildcart.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import net.ausiasmarch.wildcart.repository.TipousuarioRepository;

@Service
public class UsuarioService {

    @Autowired
    TipousuarioService oTipousuarioService;
    @Autowired
    TipousuarioRepository oTipousuarioRepository;

    @Autowired
    UsuarioRepository oUsuarioRepository;

    @Autowired
    AuthService oAuthService;

    private final String DNI_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private final String WILDCART_DEFAULT_PASSWORD = "4298f843f830fb3cc13ecdfe1b2cf10f51f929df056d644d1bca73228c5e8f64"; //wildcart
    private final String[] NAMES = {"Jose", "Mark", "Elen", "Toni", "Hector", "Jose", "Laura", "Vika", "Sergio",
        "Javi", "Marcos", "Pere", "Daniel", "Jose", "Javi", "Sergio", "Aaron", "Rafa", "Lionel", "Borja"};

    private final String[] SURNAMES = {"Penya", "Tatay", "Coronado", "Cabanes", "Mikayelyan", "Gil", "Martinez",
        "Bargues", "Raga", "Santos", "Sierra", "Arias", "Santos", "Kuvshinnikova", "Cosin", "Frejo", "Marti",
        "Valcarcel", "Sesa", "Lence", "Villanueva", "Peyro", "Navarro", "Navarro", "Primo", "Gil", "Mocholi",
        "Ortega", "Dung", "Vi", "Sanchis", "Merida", "Aznar", "Aparici", "Tarazón", "Alcocer", "Salom", "Santamaría"};

    public void validate(Long id) {
        if (!oUsuarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("id " + id + " not exist");
        }
    }

    public void validate(UsuarioEntity oUsuarioEntity) {
        ValidationHelper.validateDNI(oUsuarioEntity.getDni(), "campo DNI de Usuario");
        ValidationHelper.validateStringLength(oUsuarioEntity.getNombre(), 2, 50, "campo nombre de Usuario (el campo debe tener longitud de 2 a 50 caracteres)");
        ValidationHelper.validateStringLength(oUsuarioEntity.getApellido1(), 2, 50, "campo primer apellido de Usuario (el campo debe tener longitud de 2 a 50 caracteres)");
        ValidationHelper.validateStringLength(oUsuarioEntity.getApellido2(), 2, 50, "campo segundo apellido de Usuario (el campo debe tener longitud de 2 a 50 caracteres)");
        ValidationHelper.validateEmail(oUsuarioEntity.getEmail(), " campo email de Usuario");
        ValidationHelper.validateLogin(oUsuarioEntity.getLogin(), " campo login de Usuario");
        if (oUsuarioRepository.existsByLogin(oUsuarioEntity.getLogin())) {
            throw new ValidationException("el campo Login está repetido");
        }
        ValidationHelper.validateRange(oUsuarioEntity.getDescuento(), 0, 100, "campo Descuento de la entidad Usuario (debe ser un entero entre 0 y 100)");
        oTipousuarioService.validate(oUsuarioEntity.getTipousuario().getId());
    }

    public UsuarioEntity get(Long id) {
        oAuthService.OnlyAdminsOrOwnUsersData(id);
        try {
            return oUsuarioRepository.findById(id).get();
        } catch (Exception ex) {
            throw new ResourceNotFoundException("id " + id + " not exist");
        }
    }

    public Long count() {
        oAuthService.OnlyAdmins();
        return oUsuarioRepository.count();
    }

    public Page<UsuarioEntity> getPage(Pageable oPageable, String strFilter, Long lTipoUsuario) {
        oAuthService.OnlyAdmins();
        Page<UsuarioEntity> oPage = null;
        if (lTipoUsuario != null) {
            if (strFilter != null) {
                oPage = oUsuarioRepository.findByTipousuarioIdAndDniIgnoreCaseContainingOrNombreIgnoreCaseContainingOrApellido1IgnoreCaseContainingOrApellido2IgnoreCaseContaining(
                        lTipoUsuario, strFilter, strFilter, strFilter, strFilter, oPageable);
            } else {
                oPage = oUsuarioRepository.findByTipousuarioId(lTipoUsuario, oPageable);
            }
        } else {
            if (strFilter != null) {
                oPage = oUsuarioRepository.findByDniIgnoreCaseContainingOrNombreIgnoreCaseContainingOrApellido1IgnoreCaseContainingOrApellido2IgnoreCaseContaining(
                        strFilter, strFilter, strFilter, strFilter, oPageable);
            } else {
                oPage = oUsuarioRepository.findAll(oPageable);
            }
        }
        return oPage;
    }

    public UsuarioEntity create(UsuarioEntity oNewUsuarioEntity) {
        oAuthService.OnlyAdmins();
        validate(oNewUsuarioEntity);
        oNewUsuarioEntity.setId(0L);
        oNewUsuarioEntity.setPassword(WILDCART_DEFAULT_PASSWORD); //wildcart
        oNewUsuarioEntity.setToken(RandomHelper.getToken(100));
        return oUsuarioRepository.save(oNewUsuarioEntity);
    }

    public UsuarioEntity update(Long id, UsuarioEntity oUsuarioEntity) {
        oAuthService.OnlyAdminsOrOwnUsersData(id);
        validate(oUsuarioEntity.getId());
        validate(oUsuarioEntity);
        if (oAuthService.isAdmin()) {
            return update4Admins(id, oUsuarioEntity);
        } else {
            return update4Users(id, oUsuarioEntity);
        }
    }

    private UsuarioEntity update4Admins(Long id, UsuarioEntity oUpdatedUsuarioEntity) {
        oUpdatedUsuarioEntity.setId(id);
        validate(oUpdatedUsuarioEntity);
        UsuarioEntity oUsuarioEntity = oUsuarioRepository.findById(id).get();
        oUpdatedUsuarioEntity.setPassword(oUsuarioEntity.getPassword());
        oUpdatedUsuarioEntity.setToken(oUsuarioEntity.getToken());
        return oUsuarioRepository.save(oUpdatedUsuarioEntity);
    }

    private UsuarioEntity update4Users(Long id, UsuarioEntity oUpdatedUsuarioEntity) {
        oUpdatedUsuarioEntity.setId(id);
        validate(oUpdatedUsuarioEntity);
        UsuarioEntity oUsuarioEntity = oUsuarioRepository.findById(id).get();
        oUpdatedUsuarioEntity.setPassword(oUsuarioEntity.getPassword());
        oUpdatedUsuarioEntity.setToken(oUsuarioEntity.getToken());
        oUpdatedUsuarioEntity.setTipousuario(oUsuarioEntity.getTipousuario());
        oUpdatedUsuarioEntity.setActivo(oUsuarioEntity.isActivo());
        oUpdatedUsuarioEntity.setValidado(oUsuarioEntity.isValidado());
        oUpdatedUsuarioEntity.setDescuento(oUsuarioEntity.getDescuento());
        return oUsuarioRepository.save(oUpdatedUsuarioEntity);
    }

    public Long delete(Long id) {
        oAuthService.OnlyAdmins();
        if (oUsuarioRepository.existsById(id)) {
            oUsuarioRepository.deleteById(id);
            if (oUsuarioRepository.existsById(id)) {
                throw new ResourceNotModifiedException("can't remove register " + id);
            } else {
                return id;
            }
        } else {
            throw new ResourceNotModifiedException("id " + id + " not exist");
        }
    }

    public UsuarioEntity generate() {
        oAuthService.OnlyAdmins();
        return generateRandomUser();
    }

    public Long generateSome(Integer amount) {
        oAuthService.OnlyAdmins();
        List<UsuarioEntity> userList = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            UsuarioEntity oUsuarioEntity = generateRandomUser();
            oUsuarioRepository.save(oUsuarioEntity);
            userList.add(oUsuarioEntity);
        }
        return oUsuarioRepository.count();
    }

    public UsuarioEntity getOneRandom() {
        if (count() > 0) {
            UsuarioEntity oUsuarioEntity = null;
            int iPosicion = RandomHelper.getRandomInt(0, (int) oUsuarioRepository.count() - 1);
            Pageable oPageable = PageRequest.of(iPosicion, 1);
            Page<UsuarioEntity> usuarioPage = oUsuarioRepository.findAll(oPageable);
            List<UsuarioEntity> usuarioList = usuarioPage.getContent();
            oUsuarioEntity = oUsuarioRepository.getById(usuarioList.get(0).getId());
            return oUsuarioEntity;
        } else {
            throw new CannotPerformOperationException("ho hay usuarios en la base de datos");
        }
    }

    private UsuarioEntity generateRandomUser() {
        UsuarioEntity oUserEntity = new UsuarioEntity();
        oUserEntity.setDni(generateDNI());
        oUserEntity.setNombre(generateName());
        oUserEntity.setApellido1(generateSurname());
        oUserEntity.setApellido2(generateSurname());
        oUserEntity.setLogin(oUserEntity.getNombre() + "_" + oUserEntity.getApellido1());
        oUserEntity.setPassword(WILDCART_DEFAULT_PASSWORD); // wildcart
        oUserEntity.setEmail(generateEmail(oUserEntity.getNombre(), oUserEntity.getApellido1()));
        oUserEntity.setDescuento(RandomHelper.getRandomInt(0, 51));
        if (RandomHelper.getRandomInt(0, 10) > 1) {
            oUserEntity.setTipousuario(oTipousuarioRepository.getById(TipoUsuarioHelper.USER));
        } else {
            oUserEntity.setTipousuario(oTipousuarioRepository.getById(TipoUsuarioHelper.ADMIN));
        }
        oUserEntity.setValidado(false);
        oUserEntity.setActivo(false);
        return oUserEntity;
    }

    private String generateDNI() {
        String dni = "";
        int dniNumber = RandomHelper.getRandomInt(11111111, 99999999 + 1);
        dni += dniNumber + "" + DNI_LETTERS.charAt(dniNumber % 23);
        return dni;
    }

    private String generateName() {
        return NAMES[RandomHelper.getRandomInt(0, NAMES.length - 1)].toLowerCase();
    }

    private String generateSurname() {
        return SURNAMES[RandomHelper.getRandomInt(0, SURNAMES.length - 1)].toLowerCase();
    }

    private String generateEmail(String name, String surname) {
        List<String> list = new ArrayList<>();
        list.add(name);
        list.add(surname);
        return getFromList(list) + "_" + getFromList(list) + "@daw.tk";
    }

    private String getFromList(List<String> list) {
        int randomNumber = RandomHelper.getRandomInt(0, list.size() - 1);
        String value = list.get(randomNumber);
        list.remove(randomNumber);
        return value;
    }

}
