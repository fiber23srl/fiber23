import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  runApp(const Fiber23App());
}

class Fiber23App extends StatelessWidget {
  const Fiber23App({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Fiber23',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        fontFamily: 'Roboto',
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF0868F7),
          primary: const Color(0xFF0868F7),
          secondary: const Color(0xFFE12DFF),
          brightness: Brightness.light,
        ),
      ),
      home: const AuthGate(),
    );
  }
}

class ApiClient {
  static const String baseUrl = 'https://www.fiber23.it/mobile';

  String? token;

  Future<Map<String, dynamic>> post(String path, Map<String, dynamic> body) async {
    final response = await http.post(
      Uri.parse('$baseUrl/$path'),
      headers: _headers(),
      body: jsonEncode(body),
    );
    return _decode(response);
  }

  Future<Map<String, dynamic>> get(String path) async {
    final response = await http.get(
      Uri.parse('$baseUrl/$path'),
      headers: _headers(),
    );
    return _decode(response);
  }

  Map<String, String> _headers() {
    return {
      'Content-Type': 'application/json',
      if (token != null && token!.isNotEmpty) 'Authorization': 'Bearer $token',
    };
  }

  Map<String, dynamic> _decode(http.Response response) {
    final data = jsonDecode(response.body) as Map<String, dynamic>;
    if (response.statusCode >= 400 || data['success'] != true) {
      throw Exception(data['message'] ?? 'Errore API Fiber23');
    }
    return data['data'] as Map<String, dynamic>? ?? {};
  }
}

final api = ApiClient();

class AuthGate extends StatefulWidget {
  const AuthGate({super.key});

  @override
  State<AuthGate> createState() => _AuthGateState();
}

class _AuthGateState extends State<AuthGate> {
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadToken();
  }

  Future<void> _loadToken() async {
    final prefs = await SharedPreferences.getInstance();
    api.token = prefs.getString('token');
    setState(() => loading = false);
  }

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return api.token == null ? const LoginScreen() : const HomeScreen();
  }
}

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final email = TextEditingController();
  final password = TextEditingController();
  bool loading = false;
  String? error;

  Future<void> login() async {
    setState(() {
      loading = true;
      error = null;
    });

    try {
      final data = await api.post('login', {
        'email': email.text.trim(),
        'password': password.text,
      });
      api.token = data['token'] as String;
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('token', api.token!);

      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (_) => const HomeScreen()),
      );
    } catch (e) {
      setState(() => error = e.toString().replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF061C3F), Color(0xFF075EEA), Color(0xFF6D23DD)],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 430),
                child: Card(
                  elevation: 18,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
                  child: Padding(
                    padding: const EdgeInsets.all(26),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const _LogoBlock(),
                        const SizedBox(height: 28),
                        TextField(
                          controller: email,
                          keyboardType: TextInputType.emailAddress,
                          decoration: const InputDecoration(
                            labelText: 'Email area clienti',
                            prefixIcon: Icon(Icons.email_outlined),
                            border: OutlineInputBorder(),
                          ),
                        ),
                        const SizedBox(height: 14),
                        TextField(
                          controller: password,
                          obscureText: true,
                          decoration: const InputDecoration(
                            labelText: 'Password',
                            prefixIcon: Icon(Icons.lock_outline),
                            border: OutlineInputBorder(),
                          ),
                        ),
                        if (error != null) ...[
                          const SizedBox(height: 14),
                          Text(error!, style: const TextStyle(color: Colors.red, fontWeight: FontWeight.w700)),
                        ],
                        const SizedBox(height: 20),
                        FilledButton.icon(
                          onPressed: loading ? null : login,
                          icon: loading
                              ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                              : const Icon(Icons.login),
                          label: const Text('Accedi'),
                          style: FilledButton.styleFrom(
                            minimumSize: const Size.fromHeight(54),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                          ),
                        ),
                        const SizedBox(height: 16),
                        const Text(
                          'Usa le stesse credenziali della tua area clienti Fiber23.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Color(0xFF63708A)),
                        ),
                        const SizedBox(height: 8),
                        const Text(
                          'v1.0.3 - API www.fiber23.it',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Color(0xFF94A3B8), fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _LogoBlock extends StatelessWidget {
  const _LogoBlock();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Wrap(
          spacing: 5,
          runSpacing: 5,
          children: List.generate(
            9,
            (index) => Container(
              width: 9,
              height: 9,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(3),
                gradient: const LinearGradient(colors: [Color(0xFF7B1CFF), Color(0xFFFF30D5)]),
              ),
            ),
          ),
        ),
        const SizedBox(height: 10),
        RichText(
          text: const TextSpan(
            style: TextStyle(fontSize: 34, fontWeight: FontWeight.w900, letterSpacing: -1),
            children: [
              TextSpan(text: 'Fiber', style: TextStyle(color: Color(0xFF6A20FF))),
              TextSpan(text: '23', style: TextStyle(color: Color(0xFFFF2AD4))),
            ],
          ),
        ),
        const Text('Area Clienti', style: TextStyle(color: Color(0xFF0B63D8), fontWeight: FontWeight.w700)),
      ],
    );
  }
}

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int index = 0;

  final pages = const [
    DashboardPage(),
    ServicesPage(),
    InvoicesPage(),
    TicketsPage(),
  ];

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('token');
    api.token = null;
    if (!mounted) return;
    Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const LoginScreen()));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Fiber23'),
        actions: [
          IconButton(onPressed: logout, icon: const Icon(Icons.logout), tooltip: 'Esci'),
        ],
      ),
      body: pages[index],
      bottomNavigationBar: NavigationBar(
        selectedIndex: index,
        onDestinationSelected: (value) => setState(() => index = value),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.dashboard_outlined), selectedIcon: Icon(Icons.dashboard), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.dns_outlined), selectedIcon: Icon(Icons.dns), label: 'Servizi'),
          NavigationDestination(icon: Icon(Icons.receipt_long_outlined), selectedIcon: Icon(Icons.receipt_long), label: 'Fatture'),
          NavigationDestination(icon: Icon(Icons.support_agent_outlined), selectedIcon: Icon(Icons.support_agent), label: 'Ticket'),
        ],
      ),
    );
  }
}

class DashboardPage extends StatelessWidget {
  const DashboardPage({super.key});

  @override
  Widget build(BuildContext context) {
    return FutureView(
      loader: () => api.get('me'),
      builder: (data) {
        final client = data['client'] as Map<String, dynamic>? ?? {};
        final name = '${client['firstname'] ?? ''} ${client['lastname'] ?? ''}'.trim();
        return ListView(
          padding: const EdgeInsets.all(18),
          children: [
            _HeroCard(
              title: name.isEmpty ? 'Area Clienti Fiber23' : 'Ciao, $name',
              subtitle: 'Gestisci servizi, fatture e ticket dal tuo telefono.',
            ),
            const SizedBox(height: 16),
            _InfoTile(icon: Icons.email_outlined, title: 'Email', value: '${client['email'] ?? '-'}'),
            _InfoTile(icon: Icons.business_outlined, title: 'Azienda', value: '${client['companyname'] ?? '-'}'),
            _InfoTile(icon: Icons.account_balance_wallet_outlined, title: 'Credito', value: '${client['credit'] ?? '-'}'),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: () {},
              icon: const Icon(Icons.add_comment_outlined),
              label: const Text('Apri nuovo ticket'),
            ),
          ],
        );
      },
    );
  }
}

class ServicesPage extends StatelessWidget {
  const ServicesPage({super.key});

  @override
  Widget build(BuildContext context) {
    return FutureView(
      loader: () => api.get('services'),
      builder: (data) {
        final products = (((data['products'] ?? {}) as Map)['product'] ?? []) as List? ?? [];
        return _SimpleList(
          empty: 'Nessun servizio trovato.',
          items: products,
          icon: Icons.dns,
          title: (item) => '${item['name'] ?? item['groupname'] ?? 'Servizio'}',
          subtitle: (item) => 'Stato: ${item['status'] ?? '-'}',
        );
      },
    );
  }
}

class InvoicesPage extends StatelessWidget {
  const InvoicesPage({super.key});

  @override
  Widget build(BuildContext context) {
    return FutureView(
      loader: () => api.get('invoices'),
      builder: (data) {
        final invoices = (((data['invoices'] ?? {}) as Map)['invoice'] ?? []) as List? ?? [];
        return _SimpleList(
          empty: 'Nessuna fattura trovata.',
          items: invoices,
          icon: Icons.receipt_long,
          title: (item) => 'Fattura #${item['id'] ?? '-'} - ${item['total'] ?? ''}',
          subtitle: (item) => 'Stato: ${item['status'] ?? '-'} · Scadenza: ${item['duedate'] ?? '-'}',
        );
      },
    );
  }
}

class TicketsPage extends StatelessWidget {
  const TicketsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return FutureView(
      loader: () => api.get('tickets'),
      builder: (data) {
        final tickets = (((data['tickets'] ?? {}) as Map)['ticket'] ?? []) as List? ?? [];
        return _SimpleList(
          empty: 'Nessun ticket trovato.',
          items: tickets,
          icon: Icons.support_agent,
          title: (item) => '#${item['tid'] ?? item['id'] ?? '-'} ${item['subject'] ?? 'Ticket'}',
          subtitle: (item) => 'Stato: ${item['status'] ?? '-'} · Reparto: ${item['deptname'] ?? '-'}',
        );
      },
    );
  }
}

class FutureView extends StatefulWidget {
  final Future<Map<String, dynamic>> Function() loader;
  final Widget Function(Map<String, dynamic>) builder;

  const FutureView({super.key, required this.loader, required this.builder});

  @override
  State<FutureView> createState() => _FutureViewState();
}

class _FutureViewState extends State<FutureView> {
  late Future<Map<String, dynamic>> future;

  @override
  void initState() {
    super.initState();
    future = widget.loader();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Map<String, dynamic>>(
      future: future,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Text(snapshot.error.toString().replaceFirst('Exception: ', ''), textAlign: TextAlign.center),
            ),
          );
        }
        return widget.builder(snapshot.data ?? {});
      },
    );
  }
}

class _HeroCard extends StatelessWidget {
  final String title;
  final String subtitle;

  const _HeroCard({required this.title, required this.subtitle});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(colors: [Color(0xFF075EEA), Color(0xFF7A2BFF), Color(0xFFE12DFF)]),
        boxShadow: [BoxShadow(color: const Color(0xFF075EEA).withOpacity(.24), blurRadius: 28, offset: const Offset(0, 16))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.verified_user_outlined, color: Colors.white, size: 34),
          const SizedBox(height: 14),
          Text(title, style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w900)),
          const SizedBox(height: 8),
          Text(subtitle, style: const TextStyle(color: Colors.white70, fontSize: 15, height: 1.5)),
        ],
      ),
    );
  }
}

class _InfoTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String value;

  const _InfoTile({required this.icon, required this.title, required this.value});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
        title: Text(title),
        subtitle: Text(value),
      ),
    );
  }
}

class _SimpleList extends StatelessWidget {
  final String empty;
  final List items;
  final IconData icon;
  final String Function(Map item) title;
  final String Function(Map item) subtitle;

  const _SimpleList({
    required this.empty,
    required this.items,
    required this.icon,
    required this.title,
    required this.subtitle,
  });

  @override
  Widget build(BuildContext context) {
    if (items.isEmpty) {
      return Center(child: Text(empty));
    }
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      separatorBuilder: (_, __) => const SizedBox(height: 8),
      itemBuilder: (context, index) {
        final item = (items[index] as Map).cast<String, dynamic>();
        return Card(
          child: ListTile(
            leading: Icon(icon, color: Theme.of(context).colorScheme.primary),
            title: Text(title(item), style: const TextStyle(fontWeight: FontWeight.w800)),
            subtitle: Text(subtitle(item)),
          ),
        );
      },
    );
  }
}
