package br.com.guilhermando.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.google.gson.Gson;

public class DB {
    private static final String DATABASE_URL = "jdbc:sqlite:data/usuarios.db";
    private static final Gson gson = new Gson();

    
    public static void initializeDatabase() {
        File diretorio = new File("data");
            if (!diretorio.exists()) {
                diretorio.mkdirs();
            }
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
        Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA busy_timeout=5000;");

            String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios (" +
                         " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         " usuario TEXT NOT NULL UNIQUE," +
                         " senha TEXT NOT NULL," +
                         " funcao TEXT NOT NULL DEFAULT 'user'" +
                         ");";
            stmt.execute(sqlUsuarios);

            String sqlFilmes = "CREATE TABLE IF NOT EXISTS filmes (" +
                        " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " titulo TEXT NOT NULL," +
                        " diretor TEXT NOT NULL," +
                        " ano TEXT NOT NULL," +
                        " genero TEXT NOT NULL," +
                        " sinopse TEXT (250)," +
                        " nota REAL DEFAULT 0.0," +
                        " UNIQUE(titulo, diretor, ano)" +
                        ");";
            stmt.execute(sqlFilmes);
            
            String sqlReviews = "CREATE TABLE IF NOT EXISTS reviews (" +
                         " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         " id_filme INTEGER NOT NULL," +
                         " id_usuario INTEGER NOT NULL," +
                         " titulo TEXT," +
                         " nota INTEGER NOT NULL," + 
                         " comentario TEXT," +
                         " data TEXT," + 
                         " editado BOOLEAN NOT NULL DEFAULT false," +
                         " FOREIGN KEY(id_filme) REFERENCES filmes(id)," +
                         " FOREIGN KEY(id_usuario) REFERENCES usuarios(id)" +
                         ");";
            stmt.execute(sqlReviews);
            System.out.println("Banco de dados inicializado e tabela 'usuarios' pronta.");

            if (!userExists("admin")) {
                 addUser("admin", "admin", "admin");
                 addUser("usuario", "usuario", "user");
            }

            cadastrarFilmesIniciais();

        } catch (SQLException e) {
            System.err.println("Erro ao inicializar o banco de dados: " + e.getMessage());
        }
    }

    public static void addUser(String usuario, String senha) {
        addUser(usuario, senha, "user");
    }

    public static void addUser(String usuario, String senha, String funcao) {
        String sql = "INSERT INTO usuarios(usuario, senha, funcao) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            pstmt.setString(2, senha);
            pstmt.setString(3, funcao);
            pstmt.executeUpdate();
            System.out.println("Usuário '" + usuario + "' ("+ funcao +") adicionado com sucesso.");
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar usuário: " + e.getMessage());
        }
    }

    public static boolean deleteReview(int idReview) {
        int idFilme = getFilmeIdByReviewId(idReview);
        
        String sql = "DELETE FROM reviews WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idReview);
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0 && idFilme != -1) {
                atualizarNotaFilme(idFilme);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar review: " + e.getMessage());
            return false;
        }
    }

    public static boolean userExists(String usuario) {
        String sql = "SELECT id FROM usuarios WHERE usuario = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Erro ao verificar usuário: " + e.getMessage());
            return false;
        }
    }

    public static Map<String, String> validateLogin(String usuario, String senha) {
        String sql = "SELECT id, usuario, funcao FROM usuarios WHERE usuario = ? AND senha = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, usuario);
            pstmt.setString(2, senha);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Map<String, String> userData = new HashMap<>();
                userData.put("id", rs.getString(("id"))); 
                userData.put("usuario", rs.getString("usuario"));
                userData.put("funcao", rs.getString("funcao"));
                return userData;
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Erro ao validar login: " + e.getMessage());
            return null;
        }
    }

    
    public static List<Map<String, String>> getAllUsers() {
        List<Map<String, String>> users = new ArrayList<>();
        
        String sql = "SELECT id, usuario FROM usuarios ORDER BY usuario ASC";
        
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, String> user = new HashMap<>();
                user.put("id", String.valueOf(rs.getInt("id")));
                user.put("nome", rs.getString("usuario"));
                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuários: " + e.getMessage());
        }
        return users;
    }

    public static boolean deleteReviewById(int idReview) {
        String sql = "DELETE FROM reviews WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idReview);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar review ID " + idReview + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteUser(String usuario) {
        String sql = "DELETE FROM usuarios WHERE usuario = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuario);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao deletar usuário " + usuario + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean updatePassword(String usuario, String novaSenha){
        String sql = "UPDATE usuarios SET senha = ? WHERE usuario = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, novaSenha);
            pstmt.setString(2, usuario);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }catch (SQLException e){
            System.err.println("Erro ao atualizar a senha do usuario " + usuario + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean filmeExists(String titulo, String diretor, String ano) {
        String sql = "SELECT id FROM filmes WHERE titulo = ? AND diretor = ? AND ano = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, titulo);
            pstmt.setString(2, diretor);
            pstmt.setString(3, ano);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); 
        } catch (SQLException e) {
            System.err.println("Erro ao verificar filme: " + e.getMessage());
            return false;
        }
    }

    public static boolean reviewExists(int idFilme, int idUsuario) {
        String sql = "SELECT id FROM reviews WHERE id_filme = ? AND id_usuario = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idFilme);
            pstmt.setInt(2, idUsuario);
            ResultSet rs = pstmt.executeQuery();
            return rs.next(); 
        } catch (SQLException e) {
            System.err.println("Erro ao verificar review: " + e.getMessage());
            return false;
        }
    }

    public static boolean addFilme(Map<String, String> dadosFilme) {
        String sql = "INSERT INTO filmes(titulo, ano, diretor, genero, sinopse) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dadosFilme.get("titulo"));
            pstmt.setString(3, dadosFilme.get("diretor"));
            pstmt.setString(2, dadosFilme.get("ano"));
            pstmt.setString(4, dadosFilme.get("genero"));
            pstmt.setString(5, dadosFilme.get("sinopse"));
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar filme: " + e.getMessage());
            return false;
        }
    }

    public static boolean addReview(int idFilme, int idUsuario, String titulo, int nota, String comentario) {
        String sql = "INSERT INTO reviews(id_filme, id_usuario, titulo, nota, comentario, data) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            pstmt.setInt(1, idFilme);
            pstmt.setInt(2, idUsuario);
            pstmt.setString(3, titulo);
            pstmt.setInt(4, nota);
            pstmt.setString(5, comentario);
            pstmt.setString(6, dataAtual); 

            pstmt.executeUpdate();
            atualizarNotaFilme(idFilme);
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao adicionar review: " + e.getMessage());
            return false;
        }
    }

    public static boolean isReviewOwner(int idReview, int idUsuario) {
        String sql = "SELECT id FROM reviews WHERE id = ? AND id_usuario = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idReview);
            pstmt.setInt(2, idUsuario);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean updateReviewById(int idReview, String novoTitulo, int novaNota, String novoComentario) {
        String sql = "UPDATE reviews SET titulo = ?, nota = ?, comentario = ?, editado = true WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, novoTitulo);     
            pstmt.setInt(2, novaNota);          
            pstmt.setString(3, novoComentario); 
            pstmt.setInt(4, idReview);          
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                int idFilme = getFilmeIdByReviewId(idReview);
                if (idFilme != -1) atualizarNotaFilme(idFilme);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar review: " + e.getMessage());
            return false;
        }
    }

    private static int getFilmeIdByReviewId(int idReview) {
        String sql = "SELECT id_filme FROM reviews WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idReview);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("id_filme");
        } catch (SQLException e) {}
        return -1;
    }

    private static void atualizarNotaFilme(int idFilme) {
        String sqlMedia = "SELECT AVG(nota) as media FROM reviews WHERE id_filme = ?";
        String sqlUpdate = "UPDATE filmes SET nota = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL)) {
            double novaNota = 0.0;
            try (PreparedStatement pstmt = conn.prepareStatement(sqlMedia)) {
                pstmt.setInt(1, idFilme);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    novaNota = rs.getObject("media") != null ? rs.getDouble("media") : 0.0;
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                pstmt.setDouble(1, novaNota);
                pstmt.setInt(2, idFilme);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar nota do filme: " + e.getMessage());
        }
    }

    public static boolean deleteFilme(int idFilme) {
        String sqlDeleteReviews = "DELETE FROM reviews WHERE id_filme = ?";
        String sqlDeleteFilme = "DELETE FROM filmes WHERE id = ?";
        
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DATABASE_URL);
            conn.setAutoCommit(false); 

            try (PreparedStatement pstmtReviews = conn.prepareStatement(sqlDeleteReviews)) {
                pstmtReviews.setInt(1, idFilme);
                pstmtReviews.executeUpdate();
            }
            
            try (PreparedStatement pstmtFilme = conn.prepareStatement(sqlDeleteFilme)) {
                pstmtFilme.setInt(1, idFilme);
                int affectedRows = pstmtFilme.executeUpdate();
                
                if (affectedRows == 0) {
                    throw new SQLException("Filme não encontrado, deleção falhou.");
                }
            }
            
            conn.commit(); 
            return true;

        } catch (SQLException e) {
            System.err.println("Erro ao deletar filme (transação falhou): " + e.getMessage());
            try {
                if (conn != null) conn.rollback(); 
            } catch (SQLException ex) {
                System.err.println("Erro ao reverter rollback: " + ex.getMessage());
            }
            return false;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }

    public static Map<String, Object> getFilmeById(int idFilme) {
        String sql = "SELECT * FROM filmes WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idFilme);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> filme = new HashMap<>();
                filme.put("id", String.valueOf(rs.getInt("id")));
                filme.put("titulo", rs.getString("titulo"));
                filme.put("ano", rs.getString("ano"));
                filme.put("diretor", rs.getString("diretor"));
                filme.put("genero", rs.getString("genero"));
                filme.put("sinopse", rs.getString("sinopse"));
                
                filme.put("nota", String.valueOf(rs.getDouble("nota")));
                
                String sqlCount = "SELECT COUNT(*) as total FROM reviews WHERE id_filme = ?";
                try (PreparedStatement pstmtCount = conn.prepareStatement(sqlCount)) {
                    pstmtCount.setInt(1, idFilme);
                    ResultSet rsCount = pstmtCount.executeQuery();
                    filme.put("qtd_avaliacoes", rsCount.next() ? String.valueOf(rsCount.getInt("total")) : "0");
                }

                return filme;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar filme por ID: " + e.getMessage());
        }
        return null;
    }

    public static boolean updateFilme(Map<String, Object> filme) {
        String sql = "UPDATE filmes SET titulo = ?, ano = ?, diretor = ?, genero = ?, sinopse = ? " +
                     "WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, (String) filme.get("titulo"));
            pstmt.setString(2, (String) filme.get("ano"));
            pstmt.setString(3, (String) filme.get("diretor"));
            pstmt.setString(4, (String) filme.get("genero"));
            pstmt.setString(5, (String) filme.get("sinopse"));
            pstmt.setInt(6, (Integer) filme.get("id"));
            
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar filme: " + e.getMessage());
            return false;
        }
    }

    public static String getAllFilmes() {
        String sql = "SELECT * FROM filmes ORDER BY id";
        List<Map<String, Object>> filmes = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> filme = new HashMap<>();
                int idFilme = rs.getInt("id");
                
                filme.put("id", Integer.toString(idFilme));
                filme.put("titulo", rs.getString("titulo"));
                filme.put("diretor", rs.getString("diretor"));
                filme.put("ano", rs.getString("ano"));
                filme.put("genero", rs.getString("genero"));
                
                filme.put("nota", String.valueOf(rs.getDouble("nota"))); 
                
                filme.put("sinopse", rs.getString("sinopse"));
                
                String sqlCount = "SELECT COUNT(*) as total FROM reviews WHERE id_filme = ?";
                try (PreparedStatement pstmtCount = conn.prepareStatement(sqlCount)) {
                    pstmtCount.setInt(1, idFilme);
                    ResultSet rsCount = pstmtCount.executeQuery();
                    if (rsCount.next()) {
                        filme.put("qtd_avaliacoes", Integer.toString(rsCount.getInt("total")));
                    } else {
                        filme.put("qtd_avaliacoes", "0");
                    }
                }
                filmes.add(filme);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar filmes: " + e.getMessage());
        }
        return gson.toJson(filmes);
    }

    public static List<Map<String, String>> getReviewsByMovieId(int idFilme) {
        String sql = "SELECT r.*, u.usuario as nome_usuario " +
                     "FROM reviews r " +
                     "JOIN usuarios u ON r.id_usuario = u.id " +
                     "WHERE r.id_filme = ?";
        
        List<Map<String, String>> reviews = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, idFilme);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, String> review = new HashMap<>();
                review.put("id", String.valueOf(rs.getInt("id")));
                review.put("id_filme", String.valueOf(rs.getInt("id_filme")));
                review.put("nome_usuario", rs.getString("nome_usuario"));
                review.put("nota", String.valueOf(rs.getInt("nota")));
                review.put("titulo", rs.getString("titulo"));
                review.put("descricao", rs.getString("comentario"));
                review.put("data", rs.getString("data"));
                review.put("editado", String.valueOf(rs.getBoolean("editado")));
                reviews.add(review);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar reviews do filme: " + e.getMessage());
        }
        return reviews;
    }
    
    public static boolean filmeExistsById(int idFilme) {
        String sql = "SELECT id FROM filmes WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idFilme);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Erro ao verificar filme por ID: " + e.getMessage());
            return false;
        }
    }

    public static Integer getUserIdByUsername(String username) {
        String sql = "SELECT id FROM usuarios WHERE usuario = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar id do usuario: " + e.getMessage());
        }
        return null;
    }

    public static List<Map<String, String>> getReviewsByUserId(int idUsuario) {
        String sql = "SELECT r.*, u.usuario as nome_usuario " +
                     "FROM reviews r " +
                     "JOIN usuarios u ON r.id_usuario = u.id " +
                     "WHERE r.id_usuario = ?";

        List<Map<String, String>> reviews = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idUsuario);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Map<String, String> review = new HashMap<>();
                review.put("id", String.valueOf(rs.getInt("id")));
                review.put("id_filme", String.valueOf(rs.getInt("id_filme")));
                review.put("nome_usuario", rs.getString("nome_usuario"));
                review.put("nota", String.valueOf(rs.getInt("nota")));
                review.put("titulo", rs.getString("titulo"));
                review.put("descricao", rs.getString("comentario"));
                review.put("data", rs.getString("data"));
                review.put("editado", String.valueOf(rs.getBoolean("editado")));

                reviews.add(review);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar reviews do usuário: " + e.getMessage());
        }
        return reviews;
    }

    public static boolean deleteUserById(int idUsuario) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean updatePasswordById(int idUsuario, String novaSenha) {
        String sql = "UPDATE usuarios SET senha = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, novaSenha);
            pstmt.setInt(2, idUsuario);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    
    public static boolean userExistsById(int idUsuario) {
        String sql = "SELECT id FROM usuarios WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idUsuario);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private static void cadastrarFilmesIniciais() {
        if (!filmeExists("A Origem", "Christopher Nolan", "2010")) {
            Map<String, String> f1 = new HashMap<>();
            f1.put("titulo", "A Origem");
            f1.put("ano", "2010");
            f1.put("diretor", "Christopher Nolan");
            f1.put("genero", "Ficção Científica,Ação");
            f1.put("sinopse", "Dom Cobb é um ladrão habilidoso, o melhor na perigosa arte da extração: roubar segredos valiosos das profundezas do subconsciente.");
            addFilme(f1);
            System.out.println("Filme 'A Origem' pré-cadastrado.");
        }

        if (!filmeExists("O Poderoso Chefão", "Francis Ford Coppola", "1972")) {
            Map<String, String> f2 = new HashMap<>();
            f2.put("titulo", "O Poderoso Chefão");
            f2.put("ano", "1972");
            f2.put("diretor", "Francis Ford Coppola");
            f2.put("genero", "Drama");
            f2.put("sinopse", "O patriarca idoso de uma dinastia do crime organizado transfere o controle de seu império clandestino para seu filho relutante.");
            addFilme(f2);
            System.out.println("Filme 'O Poderoso Chefão' pré-cadastrado.");
        }

        if (!filmeExists("Toy Story", "John Lasseter", "1995")) {
            Map<String, String> f3 = new HashMap<>();
            f3.put("titulo", "Toy Story");
            f3.put("ano", "1995");
            f3.put("diretor", "John Lasseter");
            f3.put("genero", "Animação,Aventura,Comédia");
            f3.put("sinopse", "Um boneco caubói se sente ameaçado e com ciúmes quando um novo boneco de astronauta toma o seu lugar como o brinquedo favorito.");
            addFilme(f3);
            System.out.println("Filme 'Toy Story' pré-cadastrado.");
        }

        if (!filmeExists("Matrix", "Lana Wachowski, Lilly Wachowski", "1999")) {
            Map<String, String> f4 = new HashMap<>();
            f4.put("titulo", "Matrix");
            f4.put("ano", "1999");
            f4.put("diretor", "Lana Wachowski, Lilly Wachowski");
            f4.put("genero", "Ficção Científica,Ação");
            f4.put("sinopse", "Um hacker de computador aprende com rebeldes misteriosos sobre a verdadeira natureza de sua realidade e seu papel na guerra contra seus controladores.");
            addFilme(f4);
            System.out.println("Filme 'Matrix' pré-cadastrado.");
        }

        if (!filmeExists("Titanic", "James Cameron", "1997")) {
            Map<String, String> f5 = new HashMap<>();
            f5.put("titulo", "Titanic");
            f5.put("ano", "1997");
            f5.put("diretor", "James Cameron");
            f5.put("genero", "Romance,Drama");
            f5.put("sinopse", "Uma aristocrata de diciessete anos se apaixona por um artista pobre e gentil a bordo do luxuoso e malfadado R.M.S. Titanic.");
            addFilme(f5);
            System.out.println("Filme 'Titanic' pré-cadastrado.");
        }
    }

}