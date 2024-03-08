package br.com.igorbag.githubsearch.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import br.com.igorbag.githubsearch.R
import br.com.igorbag.githubsearch.data.GitHubService
import br.com.igorbag.githubsearch.domain.Repository
import br.com.igorbag.githubsearch.ui.adapter.RepositoryAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    lateinit var nomeUsuario: EditText
    lateinit var btnConfirmar: Button
    lateinit var listaRepositories: RecyclerView
    lateinit var githubApi: GitHubService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupView()
        setupListeners()
        showUserName()
        setupRetrofit()
        getAllReposByUserName()
    }

    // Metodo responsavel por realizar o setup da view e recuperar os Ids do layout
    private fun setupView() {
        nomeUsuario = findViewById(R.id.et_nome_usuario)
        btnConfirmar = findViewById(R.id.btn_confirmar)
        listaRepositories = findViewById(R.id.rv_lista_repositories)
    }

    //metodo responsavel por configurar os listeners click da tela
    private fun setupListeners() {
        btnConfirmar.setOnClickListener {
            saveUserLocal()
        }
    }

    // salvar o usuario preenchido no EditText utilizando uma SharedPreferences
    private fun saveUserLocal() {
        val nomeUsuarioDigitado = nomeUsuario.text.toString().trim()
        if (nomeUsuarioDigitado.isNotEmpty()) {
            val sharedPref = getPreferences(MODE_PRIVATE) ?: return
            with(sharedPref.edit()) {
                putString(getString(R.string.shared_pref_nome_usuario), nomeUsuarioDigitado)
                apply()
            }
            getAllReposByUserName()
        } else {
            val sharedPref = getPreferences(MODE_PRIVATE) ?: return
            with(sharedPref.edit()) {
                remove(getString(R.string.shared_pref_nome_usuario))
                apply()
            }
            setupAdapter(arrayListOf())
        }
    }

    private fun showUserName() {
        val sharedPref = getPreferences(MODE_PRIVATE) ?: return
        val nomeUsuarioSalvo = sharedPref.getString(getString(R.string.shared_pref_nome_usuario), "")
        nomeUsuarioSalvo?.let {
            nomeUsuario.setText(it)
        }
    }

    //Metodo responsavel por fazer a configuracao base do Retrofit
    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        githubApi = retrofit.create(GitHubService::class.java)
    }

    //Metodo responsavel por buscar todos os repositorios do usuario fornecido
    private fun getAllReposByUserName() {
        if (nomeUsuario.text.toString().trim().isNotEmpty()) {
            githubApi.getAllRepositoriesByUser(nomeUsuario.text.toString())
                .enqueue(object : Callback<List<Repository>> {
                    override fun onResponse(
                        call: Call<List<Repository>>,
                        response: Response<List<Repository>>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.let {
                                setupAdapter(it)
                            }
                        } else {
                            Toast
                                .makeText(
                                    applicationContext,
                                    "Erro ao tentar buscar repositórios do usuário",
                                    Toast.LENGTH_LONG
                                )
                                .show()
                        }
                    }

                    override fun onFailure(call: Call<List<Repository>>, t: Throwable) {
                        Toast
                            .makeText(
                                applicationContext,
                                "Erro ao tentar buscar repositórios do usuário",
                                Toast.LENGTH_LONG
                            )
                            .show()
                    }
                })
        }
    }

    // Metodo responsavel por realizar a configuracao do adapter
    fun setupAdapter(list: List<Repository>) {
        val repositoryAdapter = RepositoryAdapter(list)
        listaRepositories.adapter = repositoryAdapter
        repositoryAdapter.repositorioItemLister = { repository ->
            openBrowser(repository.htmlUrl)
        }
        repositoryAdapter.btnShareLister = { repository ->
            shareRepositoryLink(repository.htmlUrl)
        }
    }

    // Metodo responsavel por compartilhar o link do repositorio selecionado
    private fun shareRepositoryLink(urlRepository: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, urlRepository)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    // Metodo responsavel por abrir o browser com o link informado do repositorio
    private fun openBrowser(urlRepository: String) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(urlRepository)
            )
        )
    }
}
